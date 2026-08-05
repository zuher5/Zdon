package com.zdon.core.downloader.manager

import com.zdon.core.common.di.ApplicationScope
import com.zdon.core.common.di.Dispatcher
import com.zdon.core.common.di.ZdonDispatcher
import com.zdon.core.database.dao.DownloadDao
import com.zdon.core.datastore.UserPreferencesDataSource
import com.zdon.core.downloader.ZdonDownloadManager
import com.zdon.core.downloader.engine.DownloadExecutor
import com.zdon.core.downloader.mapper.toDomain
import com.zdon.core.downloader.mapper.toEntity
import com.zdon.core.model.DownloadItem
import com.zdon.core.model.DownloadRequest
import com.zdon.core.model.DownloadStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the download queue.
 *
 * The queue is the `downloads` table, not an in-memory list, so it survives
 * process death. [pump] is the only place that promotes `QUEUED` rows to
 * `RUNNING`, and it is guarded by a [Mutex] so the concurrency limit holds even
 * when several callers enqueue at the same moment. Every running download owns a
 * [Job] in [activeJobs]; cancelling that job kills the yt-dlp process.
 */
@Singleton
class DownloadManagerImpl @Inject constructor(
    private val downloadDao: DownloadDao,
    private val executor: DownloadExecutor,
    private val preferences: UserPreferencesDataSource,
    @ApplicationScope private val scope: CoroutineScope,
    @Dispatcher(ZdonDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : ZdonDownloadManager {

    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val pumpMutex = Mutex()

    override fun observeDownloads(): Flow<List<DownloadItem>> =
        downloadDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeActiveDownloads(): Flow<List<DownloadItem>> =
        downloadDao.observeActive().map { entities -> entities.map { it.toDomain() } }

    override fun observeDownload(id: Long): Flow<DownloadItem?> =
        downloadDao.observeById(id).map { entity -> entity?.toDomain() }

    override fun observeActiveCount(): Flow<Int> = downloadDao.observeActiveCount()

    override suspend fun enqueue(request: DownloadRequest): Long =
        enqueueAll(listOf(request)).first()

    override suspend fun enqueueAll(requests: List<DownloadRequest>): List<Long> =
        withContext(ioDispatcher) {
            val now = System.currentTimeMillis()
            val ids = requests.map { request ->
                downloadDao.insert(request.toEntity(now))
            }
            preferences.addRecentUrl(requests.first().url)
            pump()
            ids
        }

    override suspend fun pause(id: Long) = withContext(ioDispatcher) {
        // Cancelling the job makes the executor mark the row as PAUSED.
        activeJobs.remove(id)?.cancel()
        executor.terminate(id)
        val current = downloadDao.getById(id)
        if (current != null && current.status != DownloadStatus.PAUSED) {
            downloadDao.markStopped(id, DownloadStatus.PAUSED, System.currentTimeMillis())
        }
        pump()
    }

    override suspend fun resume(id: Long) = withContext(ioDispatcher) {
        val entity = downloadDao.getById(id) ?: return@withContext
        if (!entity.status.canResume) return@withContext
        downloadDao.requeue(id, entity.retryCount, System.currentTimeMillis())
        pump()
    }

    override suspend fun cancel(id: Long) = withContext(ioDispatcher) {
        activeJobs.remove(id)?.cancel()
        executor.terminate(id)
        downloadDao.markStopped(id, DownloadStatus.CANCELLED, System.currentTimeMillis())
        pump()
    }

    override suspend fun retry(id: Long) = withContext(ioDispatcher) {
        val entity = downloadDao.getById(id) ?: return@withContext
        downloadDao.requeue(id, retryCount = entity.retryCount + 1, System.currentTimeMillis())
        pump()
    }

    override suspend fun cancelAll() = withContext(ioDispatcher) {
        downloadDao.getUnfinished().forEach { entity -> cancel(entity.id) }
    }

    override suspend fun remove(id: Long) = withContext(ioDispatcher) {
        activeJobs.remove(id)?.cancel()
        executor.terminate(id)
        downloadDao.deleteById(id)
        pump()
    }

    override suspend fun clearFinished() = withContext(ioDispatcher) {
        downloadDao.deleteFinished()
    }

    override suspend fun recoverAfterProcessDeath() = withContext(ioDispatcher) {
        val demoted = downloadDao.demoteOrphanedRunning(System.currentTimeMillis())
        if (demoted > 0) {
            Timber.i("Recovered %d download(s) interrupted by process death", demoted)
        }
        pump()
    }

    override suspend fun statusOf(id: Long): DownloadStatus? =
        withContext(ioDispatcher) { downloadDao.getById(id)?.status }

    /**
     * Starts as many queued downloads as the concurrency limit allows.
     *
     * Safe to call from anywhere: it is idempotent, serialised by [pumpMutex] and
     * never blocks the caller for the duration of a download.
     */
    suspend fun pump() {
        pumpMutex.withLock {
            val limit = preferences.currentPreferences().maxConcurrentDownloads
            val running = downloadDao.countRunning()
            val slots = (limit - running).coerceAtLeast(0)
            if (slots == 0) return@withLock

            downloadDao.getNextQueued(slots).forEach { entity ->
                if (activeJobs.containsKey(entity.id)) return@forEach
                startJob(entity.id)
            }
        }
    }

    /** True when at least one download is running or queued. */
    suspend fun hasWork(): Boolean = withContext(ioDispatcher) {
        downloadDao.getUnfinished().isNotEmpty()
    }

    private fun startJob(downloadId: Long) {
        val job = scope.launch(ioDispatcher) {
            try {
                executor.execute(downloadId)
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Unexpected failure running download %d", downloadId)
            }
        }
        activeJobs[downloadId] = job
        job.invokeOnCompletion {
            activeJobs.remove(downloadId)
            // A finished slot may allow the next queued item to start.
            scope.launch(ioDispatcher) { pump() }
        }
    }
}

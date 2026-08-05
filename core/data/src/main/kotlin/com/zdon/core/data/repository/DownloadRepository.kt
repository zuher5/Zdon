package com.zdon.core.data.repository

import com.zdon.core.downloader.ZdonDownloadManager
import com.zdon.core.model.DownloadItem
import com.zdon.core.model.DownloadRequest
import kotlinx.coroutines.flow.Flow

/**
 * Repository facade over the download manager.
 *
 * Feature modules depend on this instead of on `:core:downloader` directly, which
 * keeps the service, notification and worker types out of the UI layer.
 */
interface DownloadRepository {

    fun observeDownloads(): Flow<List<DownloadItem>>

    fun observeActiveDownloads(): Flow<List<DownloadItem>>

    fun observeDownload(id: Long): Flow<DownloadItem?>

    fun observeActiveCount(): Flow<Int>

    suspend fun enqueue(request: DownloadRequest): Long

    suspend fun enqueueAll(requests: List<DownloadRequest>): List<Long>

    suspend fun pause(id: Long)

    suspend fun resume(id: Long)

    suspend fun cancel(id: Long)

    suspend fun retry(id: Long)

    suspend fun cancelAll()

    suspend fun remove(id: Long)

    suspend fun clearFinished()

    suspend fun recoverAfterProcessDeath()
}

/** Delegating implementation; the queue logic lives in [ZdonDownloadManager]. */
class DownloadRepositoryImpl(
    private val downloadManager: ZdonDownloadManager,
    private val serviceStarter: DownloadServiceStarter,
) : DownloadRepository {

    override fun observeDownloads(): Flow<List<DownloadItem>> = downloadManager.observeDownloads()

    override fun observeActiveDownloads(): Flow<List<DownloadItem>> =
        downloadManager.observeActiveDownloads()

    override fun observeDownload(id: Long): Flow<DownloadItem?> = downloadManager.observeDownload(id)

    override fun observeActiveCount(): Flow<Int> = downloadManager.observeActiveCount()

    override suspend fun enqueue(request: DownloadRequest): Long =
        downloadManager.enqueue(request).also { serviceStarter.start() }

    override suspend fun enqueueAll(requests: List<DownloadRequest>): List<Long> =
        downloadManager.enqueueAll(requests).also { serviceStarter.start() }

    override suspend fun pause(id: Long) = downloadManager.pause(id)

    override suspend fun resume(id: Long) {
        downloadManager.resume(id)
        serviceStarter.start()
    }

    override suspend fun cancel(id: Long) = downloadManager.cancel(id)

    override suspend fun retry(id: Long) {
        downloadManager.retry(id)
        serviceStarter.start()
    }

    override suspend fun cancelAll() = downloadManager.cancelAll()

    override suspend fun remove(id: Long) = downloadManager.remove(id)

    override suspend fun clearFinished() = downloadManager.clearFinished()

    override suspend fun recoverAfterProcessDeath() = downloadManager.recoverAfterProcessDeath()
}

/** Indirection so the repository does not need an Android `Context`. */
interface DownloadServiceStarter {
    fun start()
}

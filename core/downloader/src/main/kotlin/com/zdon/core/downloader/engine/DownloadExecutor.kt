package com.zdon.core.downloader.engine

import android.content.Context
import com.zdon.core.common.network.NetworkMonitor
import com.zdon.core.common.util.ArgumentSanitizer
import com.zdon.core.database.dao.DownloadDao
import com.zdon.core.database.dao.HistoryDao
import com.zdon.core.database.entity.HistoryEntity
import com.zdon.core.datastore.UserPreferencesDataSource
import com.zdon.core.downloader.R
import com.zdon.core.downloader.mapper.toDomain
import com.zdon.core.downloader.mapper.toRequest
import com.zdon.core.downloader.notification.DownloadNotificationBuilder
import com.zdon.core.downloader.storage.CookieFileProvider
import com.zdon.core.downloader.storage.DownloadStorageManager
import com.zdon.core.downloader.storage.PublishError
import com.zdon.core.downloader.storage.PublishResult
import com.zdon.core.engine.EngineOptions
import com.zdon.core.engine.YtDlpEngine
import com.zdon.core.model.DownloadErrorType
import com.zdon.core.model.DownloadOutcome
import com.zdon.core.model.DownloadStatus
import com.zdon.core.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs one download from queued to terminal state.
 *
 * Responsibilities kept here (and out of the manager) are: resolving settings
 * into [EngineOptions], throttling progress writes, publishing the finished file
 * through SAF, recording history and classifying failures.
 */
@Singleton
class DownloadExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: YtDlpEngine,
    private val downloadDao: DownloadDao,
    private val historyDao: HistoryDao,
    private val preferences: UserPreferencesDataSource,
    private val storageManager: DownloadStorageManager,
    private val cookieFileProvider: CookieFileProvider,
    private val networkMonitor: NetworkMonitor,
    private val notificationBuilder: DownloadNotificationBuilder,
) {

    /**
     * Executes the download identified by [downloadId] and returns its terminal
     * status.
     *
     * Cancellation of the calling coroutine kills the yt-dlp process; the final
     * database write happens in a [NonCancellable] block so a cancelled download
     * never stays stuck in `RUNNING`.
     */
    suspend fun execute(downloadId: Long): DownloadStatus {
        val entity = downloadDao.getById(downloadId) ?: return DownloadStatus.CANCELLED
        val settings = preferences.currentPreferences()

        if (!storageManager.canWriteTo(settings.downloadTreeUri)) {
            return failWith(
                downloadId = downloadId,
                errorType = DownloadErrorType.PERMISSION_DENIED,
                message = context.getString(
                    if (settings.hasDownloadLocation) {
                        R.string.error_permission_denied
                    } else {
                        R.string.error_no_download_folder
                    },
                ),
                retryCount = entity.retryCount,
            )
        }

        if (!networkMonitor.currentlyConnected()) {
            return failWith(
                downloadId = downloadId,
                errorType = DownloadErrorType.NETWORK,
                message = context.getString(R.string.error_network_unavailable),
                retryCount = entity.retryCount,
            )
        }

        val processId = processIdFor(downloadId)
        downloadDao.updateStatus(downloadId, DownloadStatus.RUNNING, System.currentTimeMillis())
        
        val options = buildOptions(downloadId, settings)

        val throttle = ProgressThrottle()

        val outcome = try {
            coroutineScope {
                // Progress arrives on the engine's reader thread, which cannot
                // suspend. A dedicated ticker performs the database writes so the
                // reader never blocks and the UI still updates smoothly.
                val flusher = launch {
                    while (isActive) {
                        delay(ProgressThrottle.MIN_INTERVAL_MILLIS)
                        throttle.flushIfDue(downloadDao)
                    }
                }

                try {
                    engine.download(
                        downloadId = downloadId,
                        request = entity.toRequest(),
                        options = options,
                        processId = processId,
                        onProgress = throttle::offer,
                    )
                } finally {
                    flusher.cancel()
                }
            }
        } catch (cancellation: CancellationException) {
            withContext(NonCancellable) {
                engine.cancel(processId)
                throttle.discard()
                downloadDao.markStopped(
                    downloadId,
                    DownloadStatus.PAUSED,
                    System.currentTimeMillis(),
                )
            }
            throw cancellation
        }

        return withContext(NonCancellable) {
            throttle.flush(downloadDao)
            when (outcome) {
                is DownloadOutcome.Cancelled -> {
                    storageManager.clearWorkspace(downloadId)
                    downloadDao.markStopped(
                        downloadId,
                        DownloadStatus.CANCELLED,
                        System.currentTimeMillis(),
                    )
                    DownloadStatus.CANCELLED
                }

                is DownloadOutcome.Failure -> {
                    if (outcome.errorType == DownloadErrorType.COOKIES_EXPIRED && 
                        settings.cookiesFileUri == null) {
                        Timber.w("Download %d needs authentication - cookies required", downloadId)
                    }
                    failWith(
                        downloadId = downloadId,
                        errorType = outcome.errorType,
                        message = outcome.message,
                        retryCount = entity.retryCount,
                    )
                }

                is DownloadOutcome.Success -> publishAndComplete(downloadId, outcome, settings)
            }
        }
    }

    /** Stops the yt-dlp process belonging to [downloadId], if one is running. */
    fun terminate(downloadId: Long): Boolean = engine.cancel(processIdFor(downloadId))

    private suspend fun publishAndComplete(
        downloadId: Long,
        outcome: DownloadOutcome.Success,
        settings: UserPreferences,
    ): DownloadStatus {
        val entity = downloadDao.getById(downloadId)
        val publishResult = storageManager.publish(
            downloadId = downloadId,
            treeUriString = settings.downloadTreeUri,
            primaryFileName = outcome.fileName,
        )

        return when (publishResult) {
            is PublishResult.Failure -> failWith(
                downloadId = downloadId,
                errorType = publishResult.error.toErrorType(),
                message = context.getString(publishResult.error.messageRes()),
                retryCount = entity?.retryCount ?: 0,
            )

            is PublishResult.Success -> {
                val completedAt = System.currentTimeMillis()
                downloadDao.markCompleted(
                    id = downloadId,
                    outputPath = publishResult.file.uri,
                    fileName = publishResult.file.displayName,
                    fileSizeBytes = publishResult.file.sizeBytes,
                    completedAt = completedAt,
                )
                if (entity != null) {
                    historyDao.upsert(
                        HistoryEntity(
                            url = entity.url,
                            title = entity.title,
                            uploader = entity.uploader,
                            thumbnailUrl = entity.thumbnailUrl,
                            filePath = publishResult.file.uri,
                            fileName = publishResult.file.displayName,
                            fileSizeBytes = publishResult.file.sizeBytes,
                            durationSeconds = entity.durationSeconds,
                            extractor = null,
                            wasAudioOnly = entity.extractAudio || entity.quality.isAudioOnly,
                            completedAtMillis = completedAt,
                        ),
                    )
                }
                storageManager.clearWorkspace(downloadId)
                notifyTerminal(downloadId)
                DownloadStatus.COMPLETED
            }
        }
    }

    private suspend fun failWith(
        downloadId: Long,
        errorType: DownloadErrorType,
        message: String,
        retryCount: Int,
    ): DownloadStatus {
        Timber.w("Download %d failed: %s (%s)", downloadId, errorType, message)
        downloadDao.markFailed(
            id = downloadId,
            errorType = errorType,
            message = message,
            retryCount = retryCount,
            updatedAt = System.currentTimeMillis(),
        )
        notifyTerminal(downloadId)
        return DownloadStatus.FAILED
    }

    /**
     * Posts the per-download "completed"/"failed" notification. The foreground
     * service notification disappears as soon as the queue drains, so without
     * this the user would never learn the outcome of a download they started
     * and left.
     */
    private suspend fun notifyTerminal(downloadId: Long) {
        val item = downloadDao.getById(downloadId)?.toDomain() ?: return
        notificationBuilder.notifyFinished(item, notificationBuilder.launchContentIntent())
    }

    private suspend fun buildOptions(
        downloadId: Long,
        settings: UserPreferences,
    ): EngineOptions = EngineOptions(
        workingDirectoryPath = storageManager.stagingDirectory(downloadId).absolutePath,
        temporaryDirectoryPath = storageManager.temporaryDirectory(downloadId).absolutePath,
        outputTemplate = settings.outputTemplate.ifBlank {
            UserPreferences.DEFAULT_OUTPUT_TEMPLATE
        },
        retries = settings.maxRetries.coerceAtLeast(1),
        proxyUrl = settings.proxyUrl,
        cookiesFilePath = cookieFileProvider.materialize(settings.cookiesFileUri),
        customHeaders = settings.customHttpHeaders
            ?.lineSequence()
            ?.mapNotNull(ArgumentSanitizer::sanitizeHeader)
            ?.toList()
            .orEmpty(),
        downloadArchivePath = storageManager
            .downloadArchiveFile(settings.useDownloadArchive)
            ?.absolutePath,
        useDownloadArchive = settings.useDownloadArchive,
        restrictFilenames = settings.restrictFilenames,
    )

    private fun processIdFor(downloadId: Long): String = "$PROCESS_PREFIX$downloadId"

    private fun PublishError.toErrorType(): DownloadErrorType = when (this) {
        PublishError.NO_DESTINATION, PublishError.PERMISSION_DENIED ->
            DownloadErrorType.PERMISSION_DENIED
        PublishError.DISK_FULL -> DownloadErrorType.DISK_FULL
        PublishError.NOTHING_PRODUCED -> DownloadErrorType.UNKNOWN
        PublishError.IO_ERROR -> DownloadErrorType.UNKNOWN
    }

    private fun PublishError.messageRes(): Int = when (this) {
        PublishError.NO_DESTINATION -> R.string.error_no_download_folder
        PublishError.PERMISSION_DENIED -> R.string.error_permission_denied
        PublishError.DISK_FULL -> R.string.error_disk_full
        PublishError.NOTHING_PRODUCED -> R.string.error_nothing_produced
        PublishError.IO_ERROR -> R.string.error_io
    }

    private companion object {
        const val PROCESS_PREFIX = "zdon-download-"
    }
}

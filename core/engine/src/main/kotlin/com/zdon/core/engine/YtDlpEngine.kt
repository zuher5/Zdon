package com.zdon.core.engine

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.zdon.core.common.di.Dispatcher
import com.zdon.core.common.di.ZdonDispatcher
import com.zdon.core.model.DownloadErrorType
import com.zdon.core.model.DownloadOutcome
import com.zdon.core.model.DownloadProgress
import com.zdon.core.model.DownloadRequest
import com.zdon.core.model.MediaInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.json.JSONException
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin, testable wrapper around the yt-dlp process.
 *
 * All calls are suspending and run on the IO dispatcher. Cancellation destroys
 * the underlying OS process instead of leaking it, which is what makes the
 * pause/cancel actions in the UI reliable.
 */
@Singleton
class YtDlpEngine @Inject constructor(
    private val initializer: YtDlpInitializer,
    private val commandBuilder: YtDlpCommandBuilder,
    @Dispatcher(ZdonDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Fetches metadata and available formats for [url].
     *
     * @param includePlaylist when `true`, playlist entries are enumerated.
     */
    suspend fun fetchMediaInfo(
        url: String,
        options: EngineOptions,
        includePlaylist: Boolean,
    ): MediaInfoResult = withContext(ioDispatcher) {
        val status = initializer.ensureInitialized()
        if (!status.isInitialized) {
            return@withContext MediaInfoResult.Failure(
                DownloadErrorType.BINARY_MISSING,
                status.initializationError ?: BINARY_ERROR,
            )
        }

        val request = commandBuilder.buildInfoRequest(
            url = url,
            options = options,
            flatPlaylist = includePlaylist,
            capabilities = initializer.capabilities.value,
        )
        val processId = "info-${UUID.randomUUID()}"

        try {
            val response = YoutubeDL.getInstance().execute(request, processId, false, null)
            response.err
                .takeIf { it.isNotBlank() }
                ?.let { Timber.d("yt-dlp stderr while analysing %s:\n%s", url, it) }
            val info = MediaInfoParser.parse(response.out, url)
            MediaInfoResult.Success(info)
        } catch (cancellation: CancellationException) {
            destroyQuietly(processId)
            throw cancellation
        } catch (exception: YoutubeDLException) {
            // The exception message carries yt-dlp's complete stderr; log it in
            // full for debugging while the UI gets a short, classified message.
            Timber.e(exception, "yt-dlp analysis failed for %s\n%s", url, exception.message)
            MediaInfoResult.Failure(
                ErrorClassifier.classify(exception.message),
                ErrorClassifier.extractPrimaryMessage(exception.message) ?: UNKNOWN_ERROR,
            )
        } catch (exception: JSONException) {
            Timber.e(exception, "Malformed yt-dlp JSON for %s", url)
            MediaInfoResult.Failure(DownloadErrorType.UNKNOWN, UNPARSEABLE_METADATA)
        } catch (exception: InterruptedException) {
            destroyQuietly(processId)
            MediaInfoResult.Failure(DownloadErrorType.INTERRUPTED, exception.message ?: UNKNOWN_ERROR)
        }
    }

    /**
     * Runs a download to completion.
     *
     * @param onProgress invoked on the calling coroutine's dispatcher for every
     * parsed progress line. Callers are expected to throttle persistence.
     */
    suspend fun download(
        downloadId: Long,
        request: DownloadRequest,
        options: EngineOptions,
        processId: String,
        onProgress: (DownloadProgress) -> Unit,
    ): DownloadOutcome = withContext(ioDispatcher) {
        val status = initializer.ensureInitialized()
        if (!status.isInitialized) {
            return@withContext DownloadOutcome.Failure(
                DownloadErrorType.BINARY_MISSING,
                status.initializationError ?: BINARY_ERROR,
            )
        }
        if (request.requiresFfmpeg && !status.isFfmpegAvailable) {
            return@withContext DownloadOutcome.Failure(
                DownloadErrorType.FFMPEG_MISSING,
                FFMPEG_ERROR,
            )
        }

        val ytDlpRequest = commandBuilder.buildDownloadRequest(
            request = request,
            options = options,
            capabilities = initializer.capabilities.value,
        )
        var snapshot = DownloadProgress.initial(downloadId)
        var destination: String? = null
        val startedAt = System.currentTimeMillis()

        try {
            YoutubeDL.getInstance().execute(ytDlpRequest, processId, false) { _, _, line ->
                ProgressParser.parseDestination(line)?.let { destination = it }
                ProgressParser.parse(line, snapshot)?.let { updated ->
                    snapshot = updated
                    onProgress(updated)
                }
            }

            val outputFile = destination?.let(::File)
            DownloadOutcome.Success(
                filePath = outputFile?.absolutePath ?: destination,
                fileName = outputFile?.name,
                fileSizeBytes = outputFile?.takeIf { it.exists() }?.length()
                    ?: snapshot.downloadedBytes,
                elapsedMillis = System.currentTimeMillis() - startedAt,
            )
        } catch (cancellation: CancellationException) {
            withContext(NonCancellable) { destroyQuietly(processId) }
            throw cancellation
        } catch (_: YoutubeDL.CanceledException) {
            DownloadOutcome.Cancelled
        } catch (exception: YoutubeDLException) {
            // The exception message carries yt-dlp's complete stderr; log it in
            // full for debugging while the UI gets a short, classified message.
            val message = exception.message
            Timber.e(exception, "yt-dlp download %d failed\n%s", downloadId, message)
            DownloadOutcome.Failure(
                ErrorClassifier.classify(message),
                ErrorClassifier.extractPrimaryMessage(message) ?: UNKNOWN_ERROR,
            )
        } catch (exception: InterruptedException) {
            destroyQuietly(processId)
            DownloadOutcome.Failure(
                DownloadErrorType.INTERRUPTED,
                exception.message ?: INTERRUPTED_ERROR,
            )
        }
    }

    /**
     * Terminates a running process by id. Returns `true` when a live process was
     * found and killed.
     */
    fun cancel(processId: String): Boolean = destroyQuietly(processId)

    private fun destroyQuietly(processId: String): Boolean = try {
        YoutubeDL.getInstance().destroyProcessById(processId)
    } catch (exception: IllegalStateException) {
        Timber.w(exception, "Unable to destroy process %s", processId)
        false
    }

    private companion object {
        const val BINARY_ERROR = "yt-dlp is not available"
        const val FFMPEG_ERROR = "FFmpeg is required for this download but is unavailable"
        const val UNKNOWN_ERROR = "Download failed"
        const val UNPARSEABLE_METADATA = "Could not read media information"
        const val INTERRUPTED_ERROR = "Download interrupted"
    }
}

/** Result of [YtDlpEngine.fetchMediaInfo]. */
sealed interface MediaInfoResult {
    data class Success(val mediaInfo: MediaInfo) : MediaInfoResult
    data class Failure(val errorType: DownloadErrorType, val message: String) : MediaInfoResult
}

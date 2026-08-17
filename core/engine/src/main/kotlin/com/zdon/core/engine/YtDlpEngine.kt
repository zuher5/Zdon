package com.zdon.core.engine

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.zdon.core.common.di.Dispatcher
import com.zdon.core.common.di.ZdonDispatcher
import com.zdon.core.model.DownloadErrorType
import com.zdon.core.model.DownloadOutcome
import com.zdon.core.model.DownloadProgress
import com.zdon.core.model.DownloadRequest
import com.zdon.core.model.MediaInfo
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
                status.initializationError
                    ?: context.getString(R.string.engine_error_binary_missing),
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
                ErrorClassifier.extractPrimaryMessage(exception.message)
                    ?: context.getString(R.string.engine_error_unknown),
            )
        } catch (exception: JSONException) {
            Timber.e(exception, "Malformed yt-dlp JSON for %s", url)
            MediaInfoResult.Failure(
                DownloadErrorType.UNKNOWN,
                context.getString(R.string.engine_error_unparseable_metadata),
            )
        } catch (exception: InterruptedException) {
            destroyQuietly(processId)
            MediaInfoResult.Failure(
                DownloadErrorType.INTERRUPTED,
                exception.message ?: context.getString(R.string.engine_error_interrupted),
            )
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
                status.initializationError
                    ?: context.getString(R.string.engine_error_binary_missing),
            )
        }
        if (request.requiresFfmpeg && !status.isFfmpegAvailable) {
            return@withContext DownloadOutcome.Failure(
                DownloadErrorType.FFMPEG_MISSING,
                context.getString(R.string.engine_error_ffmpeg_missing),
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
                ErrorClassifier.extractPrimaryMessage(message)
                    ?: context.getString(R.string.engine_error_unknown),
            )
        } catch (exception: InterruptedException) {
            destroyQuietly(processId)
            DownloadOutcome.Failure(
                DownloadErrorType.INTERRUPTED,
                exception.message ?: context.getString(R.string.engine_error_interrupted),
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
}

/** Result of [YtDlpEngine.fetchMediaInfo]. */
sealed interface MediaInfoResult {
    data class Success(val mediaInfo: MediaInfo) : MediaInfoResult
    data class Failure(val errorType: DownloadErrorType, val message: String) : MediaInfoResult
}

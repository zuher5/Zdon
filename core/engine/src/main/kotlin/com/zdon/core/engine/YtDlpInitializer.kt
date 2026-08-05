package com.zdon.core.engine

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.zdon.core.common.di.Dispatcher
import com.zdon.core.common.di.ZdonDispatcher
import com.zdon.core.model.BinaryUpdateResult
import com.zdon.core.model.EngineStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the lifecycle of the bundled yt-dlp and FFmpeg payloads.
 *
 * Initialisation unpacks a private Python runtime, so it is expensive and must
 * never run on the main thread. A [Mutex] guarantees a single initialisation even
 * when several downloads start at once, and the result is cached in
 * [status] so the UI can explain a missing-binary failure.
 */
@Singleton
class YtDlpInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(ZdonDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow(EngineStatus.Unknown)
    private val _capabilities = MutableStateFlow(YtDlpCapabilities.unknown())

    /** Current engine readiness; safe to collect from the UI. */
    val status: StateFlow<EngineStatus> = _status.asStateFlow()

    /**
     * The option set accepted by the installed yt-dlp binary. Populated during
     * initialisation; falls back to [YtDlpCapabilities.unknown] when the binary
     * could not be probed.
     */
    val capabilities: StateFlow<YtDlpCapabilities> = _capabilities.asStateFlow()

    /**
     * Ensures both payloads are extracted and executable.
     *
     * @return the resulting [EngineStatus]; check [EngineStatus.isInitialized]
     * before running a command.
     */
    suspend fun ensureInitialized(): EngineStatus = mutex.withLock {
        val current = _status.value
        if (current.isInitialized) return current

        val result = withContext(ioDispatcher) {
            try {
                YoutubeDL.getInstance().init(context)
                val ffmpegAvailable = initializeFfmpeg()
                val capabilities = detectCapabilities()
                _capabilities.value = capabilities
                EngineStatus(
                    isInitialized = true,
                    ytDlpVersion = capabilities.version?.toString()
                        ?: YoutubeDL.getInstance().version(context),
                    isFfmpegAvailable = ffmpegAvailable,
                    initializationError = null,
                )
            } catch (exception: YoutubeDLException) {
                Timber.e(exception, "yt-dlp initialization failed")
                EngineStatus(
                    isInitialized = false,
                    ytDlpVersion = null,
                    isFfmpegAvailable = false,
                    initializationError = exception.message ?: DEFAULT_INIT_ERROR,
                )
            } catch (error: UnsatisfiedLinkError) {
                Timber.e(error, "Native payload missing for this ABI")
                EngineStatus(
                    isInitialized = false,
                    ytDlpVersion = null,
                    isFfmpegAvailable = false,
                    initializationError = error.message ?: DEFAULT_INIT_ERROR,
                )
            }
        }

        _status.value = result
        result
    }

    /** Downloads the newest stable yt-dlp build into the app's private storage. */
    suspend fun updateYtDlp(): BinaryUpdateResult = withContext(ioDispatcher) {
        if (!ensureInitialized().isInitialized) return@withContext BinaryUpdateResult.FAILED
        try {
            val result = YoutubeDL.getInstance().updateYoutubeDL(
                context,
                YoutubeDL.UpdateChannel.STABLE,
            )
            _status.update { it.copy(ytDlpVersion = YoutubeDL.getInstance().version(context)) }
            _capabilities.value = detectCapabilities()
            when (result) {
                YoutubeDL.UpdateStatus.DONE -> BinaryUpdateResult.UPDATED
                YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> BinaryUpdateResult.ALREADY_UP_TO_DATE
                null -> BinaryUpdateResult.FAILED
            }
        } catch (exception: YoutubeDLException) {
            Timber.e(exception, "yt-dlp update failed")
            BinaryUpdateResult.FAILED
        }
    }

    /**
     * Re-extracts the bundled FFmpeg payload. FFmpeg ships inside the APK, so
     * this refreshes the on-disk copy rather than fetching a new build.
     */
    suspend fun refreshFfmpeg(): BinaryUpdateResult = withContext(ioDispatcher) {
        val available = initializeFfmpeg()
        _status.update { it.copy(isFfmpegAvailable = available) }
        if (available) BinaryUpdateResult.ALREADY_UP_TO_DATE else BinaryUpdateResult.FAILED
    }

    private fun initializeFfmpeg(): Boolean = try {
        FFmpeg.getInstance().init(context)
        true
    } catch (exception: YoutubeDLException) {
        Timber.e(exception, "FFmpeg initialization failed")
        false
    } catch (error: UnsatisfiedLinkError) {
        Timber.e(error, "FFmpeg native payload missing for this ABI")
        false
    }

    /**
     * Probes the installed binary with `yt-dlp --version` and `yt-dlp --help`
     * (in parallel) and resolves the option set it accepts. A failed probe
     * degrades to [YtDlpCapabilities.unknown] so downloads never block on it.
     */
    private suspend fun detectCapabilities(): YtDlpCapabilities {
        val probe = YtDlpProbe()
        return coroutineScope {
            val version = async { probe.version() }
            val helpOptions = async { probe.helpOptions() }
            YtDlpCapabilities.detected(version.await(), helpOptions.await())
        }
    }

    private inline fun MutableStateFlow<EngineStatus>.update(
        transform: (EngineStatus) -> EngineStatus,
    ) {
        value = transform(value)
    }

    private companion object {
        const val DEFAULT_INIT_ERROR = "Unable to prepare the bundled yt-dlp binary"
    }
}

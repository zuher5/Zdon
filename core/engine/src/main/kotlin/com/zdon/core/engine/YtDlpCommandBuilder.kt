package com.zdon.core.engine

import com.yausername.youtubedl_android.YoutubeDLRequest
import com.zdon.core.common.util.ArgumentSanitizer
import com.zdon.core.model.AudioFormat
import com.zdon.core.model.DownloadRequest
import com.zdon.core.model.VideoQuality
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central builder for every yt-dlp command.
 *
 * All yt-dlp invocations in the app go through this class, and every option is
 * routed through [YoutubeDLRequest.addIfSupported] so it is only emitted when
 * [YtDlpCapabilities] reports the installed binary accepts it. An option that
 * is unavailable for the detected yt-dlp version is omitted instead of
 * crashing the run.
 *
 * Security: every value that originates from user input passes through
 * [ArgumentSanitizer] and is added as its own argument. No string is ever
 * concatenated into a shell command, and no shell is spawned, so there is no
 * command-injection surface.
 */
@Singleton
class YtDlpCommandBuilder @Inject constructor() {

    /** Builds the command for a real download. */
    fun buildDownloadRequest(
        request: DownloadRequest,
        options: EngineOptions,
        capabilities: YtDlpCapabilities,
    ): YoutubeDLRequest = YoutubeDLRequest(request.url).apply {
        applyCommonOptions(options, capabilities)
        applyProgressOptions(capabilities)
        applyResilienceOptions(options, capabilities)
        applyFormatOptions(request, capabilities)
        applyPostProcessingOptions(request, capabilities)
        applySubtitleOptions(request, capabilities)
        applyThumbnailOptions(request, capabilities)
        applyPlaylistOptions(request, options, capabilities)
        applyOutputOptions(request, options, capabilities)
    }

    /** Builds the metadata-only command used by the analyse step. */
    fun buildInfoRequest(
        url: String,
        options: EngineOptions,
        flatPlaylist: Boolean,
        capabilities: YtDlpCapabilities,
    ): YoutubeDLRequest = YoutubeDLRequest(url).apply {
        applyCommonOptions(options, capabilities)
        applyResilienceOptions(options, capabilities)
        addIfSupported(capabilities, "--dump-single-json")
        addIfSupported(capabilities, "--no-warnings")
        if (flatPlaylist) {
            addIfSupported(capabilities, "--flat-playlist")
        } else {
            addIfSupported(capabilities, "--no-playlist")
        }
    }

    private fun YoutubeDLRequest.applyCommonOptions(
        options: EngineOptions,
        capabilities: YtDlpCapabilities,
    ) {
        // Never write to the user's real HOME; the engine sets a private cache dir.
        addIfSupported(capabilities, "--no-cache-dir")
        addIfSupported(capabilities, "--ignore-config")
        addIfSupported(capabilities, "--no-mtime")
        applyOutputStyle(capabilities)

        ArgumentSanitizer.sanitizeOptionValue(options.proxyUrl)?.let {
            addIfSupported(capabilities, "--proxy", it)
        }
        options.cookiesFilePath?.let { addIfSupported(capabilities, "--cookies", it) }
        options.customHeaders.forEach { header ->
            addIfSupported(capabilities, "--add-headers", header)
        }
        options.downloadArchivePath?.let { addIfSupported(capabilities, "--download-archive", it) }
        if (options.restrictFilenames) addIfSupported(capabilities, "--restrict-filenames")
    }

    private fun YoutubeDLRequest.applyOutputStyle(capabilities: YtDlpCapabilities) {
        // Modern yt-dlp prefers `--color no_color`; older builds expose the
        // deprecated `--no-colors` alias instead. Never pass an option the
        // installed binary does not understand.
        when {
            capabilities.supports("--color") -> addIfSupported(capabilities, "--color", "no_color")
            capabilities.supports("--no-colors") -> addIfSupported(capabilities, "--no-colors")
        }
    }

    private fun YoutubeDLRequest.applyProgressOptions(capabilities: YtDlpCapabilities) {
        addIfSupported(capabilities, "--newline")
        addIfSupported(capabilities, "--progress")
        addIfSupported(capabilities, "--progress-template", ProgressParser.PROGRESS_TEMPLATE)
    }

    private fun YoutubeDLRequest.applyResilienceOptions(
        options: EngineOptions,
        capabilities: YtDlpCapabilities,
    ) {
        // Resume partially transferred files instead of restarting from zero.
        addIfSupported(capabilities, "--continue")
        addIfSupported(capabilities, "--retries", options.retries.toString())
        addIfSupported(capabilities, "--fragment-retries", options.retries.toString())
        // Exponential backoff between retries so a flaky network or a rate-limited
        // host (TikTok anti-bot, etc.) gets a chance to recover mid-run instead of
        // failing the whole download.
        addIfSupported(capabilities, "--retry-sleep", RETRY_SLEEP_POLICY)
        addIfSupported(capabilities, "--socket-timeout", SOCKET_TIMEOUT_SECONDS.toString())
        addIfSupported(capabilities, "--no-abort-on-error")
    }

    private fun YoutubeDLRequest.applyFormatOptions(
        request: DownloadRequest,
        capabilities: YtDlpCapabilities,
    ) {
        val explicitFormat = ArgumentSanitizer.sanitizeFormatExpression(request.customFormatId)
        val maxHeight = request.quality.maxHeight
        when {
            explicitFormat != null -> addIfSupported(capabilities, "-f", explicitFormat)
            request.quality.isAudioOnly || request.extractAudio ->
                addIfSupported(capabilities, "-f", AUDIO_SELECTOR)
            // When the user asked for a TikTok-friendly file, bias the selector
            // toward H.264/AAC so the recode is a no-op when the site already
            // serves it and a cheap transcode otherwise.
            request.recodeH264 && maxHeight != null ->
                addIfSupported(capabilities, "-f", h264VideoSelector(maxHeight))
            request.recodeH264 ->
                addIfSupported(capabilities, "-f", H264_BEST_SELECTOR)
            maxHeight != null ->
                addIfSupported(capabilities, "-f", videoSelector(request.quality))
            else -> addIfSupported(capabilities, "-f", BEST_SELECTOR)
        }

        if (!request.extractAudio && !request.quality.isAudioOnly) {
            applyContainerOptions(request, capabilities)
        }
    }

    private fun YoutubeDLRequest.applyContainerOptions(
        request: DownloadRequest,
        capabilities: YtDlpCapabilities,
    ) {
        // Recode wins: it re-encodes video to H.264 and remuxes into MP4, which
        // makes any explicit container choice redundant.
        if (request.recodeH264) {
            addIfSupported(capabilities, "--recode-video", RECODE_VIDEO_TARGET)
            return
        }

        val container = if (request.container.remuxes) request.container.extension else DEFAULT_CONTAINER
        // Container to use when yt-dlp still has to merge separate video+audio
        // streams; keeps a directly playable file even without a remux.
        addIfSupported(capabilities, "--merge-output-format", container)
        // Swap the wrapper on already-muxed downloads without re-encoding.
        if (request.container.remuxes) {
            addIfSupported(capabilities, "--remux-video", container)
        }
    }

    private fun YoutubeDLRequest.applyPostProcessingOptions(
        request: DownloadRequest,
        capabilities: YtDlpCapabilities,
    ) {
        if (request.extractAudio || request.quality.isAudioOnly) {
            addIfSupported(capabilities, "--extract-audio")
            if (request.audioFormat.requiresFfmpeg) {
                addIfSupported(capabilities, "--audio-format", request.audioFormat.extension)
                addIfSupported(capabilities, "--audio-quality", DEFAULT_AUDIO_QUALITY)
            }
        }
        if (request.embedMetadata) {
            // --add-metadata is the legacy alias of --embed-metadata; fall back
            // to it only when the installed binary predates the modern name.
            if (capabilities.supports("--embed-metadata")) {
                addIfSupported(capabilities, "--embed-metadata")
            } else {
                addIfSupported(capabilities, "--add-metadata")
            }
        }
    }

    private fun YoutubeDLRequest.applySubtitleOptions(
        request: DownloadRequest,
        capabilities: YtDlpCapabilities,
    ) {
        if (!request.downloadSubtitles) return
        addIfSupported(capabilities, "--write-subs")
        addIfSupported(capabilities, "--write-auto-subs")
        val languages = ArgumentSanitizer.sanitizeLanguageList(
            request.subtitleLanguages.joinToString(","),
        )
        addIfSupported(capabilities, "--sub-langs", languages ?: DEFAULT_SUBTITLE_LANGUAGE)
        if (request.embedSubtitles) addIfSupported(capabilities, "--embed-subs")
    }

    private fun YoutubeDLRequest.applyThumbnailOptions(
        request: DownloadRequest,
        capabilities: YtDlpCapabilities,
    ) {
        if (request.downloadThumbnail) addIfSupported(capabilities, "--write-thumbnail")
        if (request.embedThumbnail) addIfSupported(capabilities, "--embed-thumbnail")
    }

    private fun YoutubeDLRequest.applyPlaylistOptions(
        request: DownloadRequest,
        options: EngineOptions,
        capabilities: YtDlpCapabilities,
    ) {
        if (request.isPlaylist) {
            addIfSupported(capabilities, "--yes-playlist")
            ArgumentSanitizer.sanitizePlaylistItems(request.playlistItems)?.let {
                addIfSupported(capabilities, "--playlist-items", it)
            }
            if (options.useDownloadArchive && options.downloadArchivePath == null) {
                // Archive support was requested but no writable path was provided;
                // fall back to skipping already-present files by name.
                addIfSupported(capabilities, "--no-overwrites")
            }
        } else {
            addIfSupported(capabilities, "--no-playlist")
        }
    }

    private fun YoutubeDLRequest.applyOutputOptions(
        request: DownloadRequest,
        options: EngineOptions,
        capabilities: YtDlpCapabilities,
    ) {
        val template = ArgumentSanitizer.sanitizeFileName(request.outputTemplate)
            ?: options.outputTemplate
        addIfSupported(capabilities, "-o", "${options.workingDirectoryPath}/$template")
        addIfSupported(capabilities, "--paths", "temp:${options.temporaryDirectoryPath}")
    }

    private fun videoSelector(quality: VideoQuality): String {
        val height = quality.maxHeight ?: return BEST_SELECTOR
        return buildString {
            append("bestvideo[height<=?$height][ext=mp4]+bestaudio[ext=m4a]/")
            append("bestvideo[height<=?$height]+bestaudio/")
            append("best[height<=?$height]/best")
        }
    }

    /**
     * Format selector that prefers H.264 video + AAC audio at or below [height].
     * When the site already serves that pair the later `--recode-video` becomes a
     * cheap remux; otherwise it falls back to any stream and the recode transcodes.
     */
    private fun h264VideoSelector(height: Int): String = buildString {
        append("bestvideo[height<=?$height][vcodec^=avc1]+bestaudio[acodec^=mp4a]/")
        append("bestvideo[height<=?$height][ext=mp4]+bestaudio[ext=m4a]/")
        append("bestvideo[height<=?$height]+bestaudio/")
        append("best[height<=?$height]/best")
    }

    /**
     * Adds [option] only when [capabilities] report the installed yt-dlp binary
     * accepts it. Unavailable options are silently omitted.
     */
    private fun YoutubeDLRequest.addIfSupported(
        capabilities: YtDlpCapabilities,
        option: String,
    ) {
        if (capabilities.supports(option)) addOption(option)
    }

    private fun YoutubeDLRequest.addIfSupported(
        capabilities: YtDlpCapabilities,
        option: String,
        argument: String,
    ) {
        if (capabilities.supports(option)) addOption(option, argument)
    }

    private companion object {
        const val BEST_SELECTOR = "bestvideo*+bestaudio/best"
        const val H264_BEST_SELECTOR =
            "bestvideo[vcodec^=avc1]+bestaudio[acodec^=mp4a]/bestvideo[ext=mp4]+bestaudio[ext=m4a]/best"
        const val AUDIO_SELECTOR = "bestaudio/best"
        const val DEFAULT_CONTAINER = "mp4"
        const val RECODE_VIDEO_TARGET = "mp4"
        const val DEFAULT_AUDIO_QUALITY = "0"
        const val DEFAULT_SUBTITLE_LANGUAGE = "en"
        const val SOCKET_TIMEOUT_SECONDS = 60
        // yt-dlp: exponential backoff 1s, 2s, 4s, 8s (then capped at 8s).
        const val RETRY_SLEEP_POLICY = "exp=1:8"
    }
}

/** Values the engine resolves once per invocation from settings and storage. */
data class EngineOptions(
    val workingDirectoryPath: String,
    val temporaryDirectoryPath: String,
    val outputTemplate: String,
    val retries: Int,
    val proxyUrl: String?,
    val cookiesFilePath: String?,
    val customHeaders: List<String>,
    val downloadArchivePath: String?,
    val useDownloadArchive: Boolean,
    val restrictFilenames: Boolean,
) {
    init {
        require(workingDirectoryPath.isNotBlank()) { "Working directory must be set" }
        require(temporaryDirectoryPath.isNotBlank()) { "Temporary directory must be set" }
        require(outputTemplate.isNotBlank()) { "Output template must be set" }
    }
}

/** Unused audio-format hook kept explicit so the enum stays exhaustive. */
internal fun AudioFormat.selectorHint(): String = extension

package com.zdon.core.model

/**
 * Everything needed to start one yt-dlp invocation. Produced by the UI and
 * persisted with the download so a queued or failed item can be re-run
 * verbatim after a process death.
 */
data class DownloadRequest(
    val url: String,
    val title: String,
    val quality: VideoQuality,
    val audioFormat: AudioFormat,
    val customFormatId: String? = null,
    val extractAudio: Boolean = false,
    val downloadSubtitles: Boolean = false,
    val subtitleLanguages: List<String> = emptyList(),
    val embedSubtitles: Boolean = false,
    val downloadThumbnail: Boolean = false,
    val embedThumbnail: Boolean = false,
    val embedMetadata: Boolean = false,
    val isPlaylist: Boolean = false,
    val playlistItems: String? = null,
    val outputTemplate: String? = null,
    val thumbnailUrl: String? = null,
    val uploader: String? = null,
    val durationSeconds: Long = 0L,
) {
    init {
        require(url.isNotBlank()) { "Download URL must not be blank" }
    }

    /** True when the resulting command needs FFmpeg to be initialised. */
    val requiresFfmpeg: Boolean
        get() = extractAudio && audioFormat.requiresFfmpeg ||
            embedSubtitles ||
            embedThumbnail ||
            embedMetadata ||
            quality != VideoQuality.AUDIO_ONLY && quality != VideoQuality.CUSTOM
}

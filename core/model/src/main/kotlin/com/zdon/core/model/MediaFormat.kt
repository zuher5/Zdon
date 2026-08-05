package com.zdon.core.model

/**
 * A single selectable output produced by yt-dlp's `--dump-json` formats array,
 * normalised into a shape the UI can render without further parsing.
 */
data class MediaFormat(
    val formatId: String,
    val extension: String,
    val formatNote: String?,
    val width: Int?,
    val height: Int?,
    val fps: Int?,
    val videoCodec: String?,
    val audioCodec: String?,
    val videoBitrateKbps: Int?,
    val audioBitrateKbps: Int?,
    val totalBitrateKbps: Int?,
    val fileSizeBytes: Long?,
    val isApproximateSize: Boolean,
) {
    /** True when the stream carries video data. */
    val hasVideo: Boolean
        get() = !videoCodec.isNullOrBlank() && videoCodec != NONE_CODEC

    /** True when the stream carries audio data. */
    val hasAudio: Boolean
        get() = !audioCodec.isNullOrBlank() && audioCodec != NONE_CODEC

    /** Audio-only streams are offered separately in the UI. */
    val isAudioOnly: Boolean
        get() = hasAudio && !hasVideo

    /** Video-only streams must be muxed with an audio stream by FFmpeg. */
    val isVideoOnly: Boolean
        get() = hasVideo && !hasAudio

    /** `1920x1080`, or `null` when the stream has no picture. */
    val resolution: String?
        get() = if (width != null && height != null && width > 0 && height > 0) {
            "${width}x$height"
        } else {
            null
        }

    /**
     * Vertical resolution used for sorting and for matching a
     * [VideoQuality] preference. Zero for audio-only streams.
     */
    val verticalResolution: Int
        get() = height ?: 0

    companion object {
        const val NONE_CODEC: String = "none"
    }
}

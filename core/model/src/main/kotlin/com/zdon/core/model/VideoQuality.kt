package com.zdon.core.model

/**
 * Quality selection offered in the UI. [BEST] and [AUDIO_ONLY] delegate the
 * decision to yt-dlp's own format sorting; the numeric entries cap the vertical
 * resolution. [CUSTOM] means the user supplied a raw yt-dlp format expression.
 */
enum class VideoQuality(val label: String, val maxHeight: Int?) {
    BEST("Best", null),
    UHD_2160P("2160p", 2160),
    QHD_1440P("1440p", 1440),
    FHD_1080P("1080p", 1080),
    HD_720P("720p", 720),
    SD_480P("480p", 480),
    LD_360P("360p", 360),
    AUDIO_ONLY("Audio only", null),
    CUSTOM("Custom", null),
    ;

    val isAudioOnly: Boolean
        get() = this == AUDIO_ONLY

    val isCustom: Boolean
        get() = this == CUSTOM

    companion object {
        /** Entries the settings screen offers as a default quality. */
        val selectableDefaults: List<VideoQuality> = listOf(
            BEST, UHD_2160P, QHD_1440P, FHD_1080P, HD_720P, SD_480P, LD_360P, AUDIO_ONLY,
        )

        fun fromNameOrDefault(name: String?, fallback: VideoQuality = BEST): VideoQuality =
            entries.firstOrNull { it.name == name } ?: fallback
    }
}

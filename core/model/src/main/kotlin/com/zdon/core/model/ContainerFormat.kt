package com.zdon.core.model

/**
 * Output container for video downloads.
 *
 * [ORIGINAL] keeps whatever yt-dlp produces. [MP4]/[MKV]/[WEBM] force a
 * container via `--remux-video` (stream copy, no re-encode). Remux alone only
 * swaps the wrapper: a VP9/AV1 stream stays VP9/AV1 inside an MP4, which some
 * mobile editors still refuse. [DownloadRequest.recodeH264] handles that by
 * re-encoding to H.264/AAC.
 */
enum class ContainerFormat(val extension: String, val label: String) {
    ORIGINAL("", "Original"),
    MP4("mp4", "MP4"),
    MKV("mkv", "MKV"),
    WEBM("webm", "WebM"),
    ;

    /** True when a container swap should be requested via `--remux-video`. */
    val remuxes: Boolean
        get() = this != ORIGINAL

    companion object {
        fun fromNameOrDefault(name: String?, fallback: ContainerFormat = MP4): ContainerFormat =
            entries.firstOrNull { it.name == name } ?: fallback
    }
}

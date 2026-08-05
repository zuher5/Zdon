package com.zdon.core.model

/**
 * Result of analysing a URL. For a playlist, [entries] holds one element per
 * playlist item and [isPlaylist] is `true`; for a single medium, [entries]
 * holds exactly one element.
 */
data class MediaInfo(
    val id: String,
    val originalUrl: String,
    val webpageUrl: String?,
    val title: String,
    val uploader: String?,
    val durationSeconds: Long,
    val viewCount: Long?,
    val likeCount: Long?,
    val thumbnailUrl: String?,
    val description: String?,
    val extractor: String?,
    val uploadDate: String?,
    val formats: List<MediaFormat>,
    val subtitleLanguages: List<String>,
    val isPlaylist: Boolean,
    val entries: List<MediaInfo>,
) {
    /** Video (or muxable video-only) formats, best first. */
    val videoFormats: List<MediaFormat>
        get() = formats.filter { it.hasVideo }
            .sortedWith(
                compareByDescending<MediaFormat> { it.verticalResolution }
                    .thenByDescending { it.fps ?: 0 }
                    .thenByDescending { it.totalBitrateKbps ?: 0 },
            )

    /** Audio-only formats, best first. */
    val audioFormats: List<MediaFormat>
        get() = formats.filter { it.isAudioOnly }
            .sortedByDescending { it.audioBitrateKbps ?: it.totalBitrateKbps ?: 0 }

    /** Distinct vertical resolutions offered by this medium, descending. */
    val availableResolutions: List<Int>
        get() = videoFormats.map { it.verticalResolution }
            .filter { it > 0 }
            .distinct()

    companion object {
        /** Placeholder used by previews and tests. */
        val Empty: MediaInfo = MediaInfo(
            id = "",
            originalUrl = "",
            webpageUrl = null,
            title = "",
            uploader = null,
            durationSeconds = 0L,
            viewCount = null,
            likeCount = null,
            thumbnailUrl = null,
            description = null,
            extractor = null,
            uploadDate = null,
            formats = emptyList(),
            subtitleLanguages = emptyList(),
            isPlaylist = false,
            entries = emptyList(),
        )
    }
}

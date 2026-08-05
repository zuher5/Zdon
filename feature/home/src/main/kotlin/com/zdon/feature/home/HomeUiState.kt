package com.zdon.feature.home

import com.zdon.core.model.AudioFormat
import com.zdon.core.model.MediaFormat
import com.zdon.core.model.MediaInfo
import com.zdon.core.model.VideoQuality

/**
 * State of the home screen. A single immutable data class keeps the Compose
 * recompositions predictable and makes the ViewModel trivially testable.
 */
data class HomeUiState(
    val urlInput: String = "",
    val isUrlValid: Boolean = false,
    val urlError: UrlInputError? = null,
    val isAnalyzing: Boolean = false,
    val mediaInfo: MediaInfo? = null,
    val analyzeError: String? = null,
    val recentUrls: List<String> = emptyList(),
    val selectedQuality: VideoQuality = VideoQuality.BEST,
    val selectedFormatId: String? = null,
    val extractAudio: Boolean = false,
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val downloadSubtitles: Boolean = false,
    val embedThumbnail: Boolean = false,
    val embedMetadata: Boolean = true,
    val downloadPlaylist: Boolean = false,
    val customFileName: String = "",
    val hasDownloadFolder: Boolean = true,
    val isEnqueueing: Boolean = false,
    val activeDownloadCount: Int = 0,
) {
    /** Video streams offered in the resolution list. */
    val videoFormats: List<MediaFormat>
        get() = mediaInfo?.videoFormats.orEmpty()

    /** Audio-only streams offered in the audio list. */
    val audioFormats: List<MediaFormat>
        get() = mediaInfo?.audioFormats.orEmpty()

    /** True once analysis has produced something the user can download. */
    val hasAnalysisResult: Boolean
        get() = mediaInfo != null

    /** The download button is enabled only when a download can really start. */
    val canDownload: Boolean
        get() = isUrlValid && !isAnalyzing && !isEnqueueing

    /** Estimated size of the current selection, in bytes, or `null` if unknown. */
    val estimatedSizeBytes: Long?
        get() {
            val info = mediaInfo ?: return null
            val explicit = selectedFormatId?.let { id ->
                info.formats.firstOrNull { it.formatId == id }?.fileSizeBytes
            }
            if (explicit != null) return explicit

            return if (extractAudio || selectedQuality.isAudioOnly) {
                info.audioFormats.firstOrNull()?.fileSizeBytes
            } else {
                val maxHeight = selectedQuality.maxHeight
                val candidates = if (maxHeight == null) {
                    info.videoFormats
                } else {
                    info.videoFormats.filter { it.verticalResolution <= maxHeight }
                }
                candidates.firstOrNull()?.fileSizeBytes
            }
        }
}

/** URL validation failures rendered next to the input field. */
enum class UrlInputError {
    MALFORMED,
    UNSUPPORTED_SCHEME,
    ILLEGAL_CHARACTERS,
}

/** One-shot events the screen consumes and clears. */
sealed interface HomeEvent {
    data class DownloadQueued(val count: Int) : HomeEvent
    data class ShowMessage(val message: String) : HomeEvent
    data object RequestDownloadFolder : HomeEvent
}

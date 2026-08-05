package com.zdon.core.model

/**
 * A row in the download manager. Mutable fields (progress, speed, status) are
 * updated in place in the database so the UI, the notification and the
 * foreground service all observe a single source of truth.
 */
data class DownloadItem(
    val id: Long,
    val url: String,
    val title: String,
    val uploader: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val status: DownloadStatus,
    val progressPercent: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Long,
    val etaSeconds: Long,
    val currentFragment: String?,
    val outputPath: String?,
    val outputFileName: String?,
    val formatId: String?,
    val quality: VideoQuality,
    val audioFormat: AudioFormat,
    val extractAudio: Boolean,
    val isPlaylist: Boolean,
    val playlistIndex: Int,
    val playlistCount: Int,
    val errorType: DownloadErrorType?,
    val errorMessage: String?,
    val retryCount: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val completedAtMillis: Long?,
    val request: DownloadRequest,
) {
    /** Bytes still to transfer, or `0` when the total size is unknown. */
    val remainingBytes: Long
        get() = if (totalBytes > downloadedBytes) totalBytes - downloadedBytes else 0L

    /** Progress in the 0f..1f range clamped for use by Compose indicators. */
    val progressFraction: Float
        get() = (progressPercent / PERCENT_SCALE).coerceIn(0f, 1f)

    val isIndeterminate: Boolean
        get() = status == DownloadStatus.RUNNING && progressPercent <= 0f

    companion object {
        const val PERCENT_SCALE: Float = 100f
        const val NO_ID: Long = 0L
    }
}

package com.zdon.core.model

/**
 * Progress emitted by the download engine while yt-dlp runs. Kept separate from
 * [DownloadItem] so high-frequency updates can be throttled before they are
 * written to the database.
 */
data class DownloadProgress(
    val downloadId: Long,
    val percent: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Long,
    val etaSeconds: Long,
    val fragment: String?,
    val rawLine: String,
) {
    companion object {
        fun initial(downloadId: Long): DownloadProgress = DownloadProgress(
            downloadId = downloadId,
            percent = 0f,
            downloadedBytes = 0L,
            totalBytes = 0L,
            speedBytesPerSecond = 0L,
            etaSeconds = -1L,
            fragment = null,
            rawLine = "",
        )
    }
}

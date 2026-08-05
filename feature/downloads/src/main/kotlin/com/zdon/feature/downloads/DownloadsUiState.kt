package com.zdon.feature.downloads

import com.zdon.core.model.DownloadItem

/** State of the downloads screen. */
data class DownloadsUiState(
    val isLoading: Boolean = true,
    val downloads: List<DownloadItem> = emptyList(),
    val filter: DownloadFilter = DownloadFilter.ALL,
) {
    /** Items matching the active filter, active work first. */
    val visibleDownloads: List<DownloadItem>
        get() = downloads
            .filter(filter::matches)
            .sortedWith(
                compareBy<DownloadItem> { it.status.sortPriority() }
                    .thenByDescending { it.createdAtMillis },
            )

    val activeCount: Int
        get() = downloads.count { it.status.isActive }

    val hasFinished: Boolean
        get() = downloads.any { it.status.isTerminal }

    val isEmpty: Boolean
        get() = !isLoading && visibleDownloads.isEmpty()
}

/** Filter chips on the downloads screen. */
enum class DownloadFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    ;

    fun matches(item: DownloadItem): Boolean = when (this) {
        ALL -> true
        ACTIVE -> item.status.isActive || item.status == com.zdon.core.model.DownloadStatus.PAUSED
        COMPLETED -> item.status == com.zdon.core.model.DownloadStatus.COMPLETED
        FAILED -> item.status == com.zdon.core.model.DownloadStatus.FAILED ||
            item.status == com.zdon.core.model.DownloadStatus.CANCELLED
    }
}

private fun com.zdon.core.model.DownloadStatus.sortPriority(): Int = when (this) {
    com.zdon.core.model.DownloadStatus.RUNNING -> 0
    com.zdon.core.model.DownloadStatus.QUEUED -> 1
    com.zdon.core.model.DownloadStatus.PAUSED -> 2
    com.zdon.core.model.DownloadStatus.FAILED -> 3
    com.zdon.core.model.DownloadStatus.COMPLETED -> 4
    com.zdon.core.model.DownloadStatus.CANCELLED -> 5
}

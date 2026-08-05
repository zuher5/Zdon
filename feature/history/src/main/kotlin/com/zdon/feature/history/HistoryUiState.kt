package com.zdon.feature.history

import com.zdon.core.model.HistoryEntry

/** State of the history screen. */
data class HistoryUiState(
    val isLoading: Boolean = true,
    val entries: List<HistoryEntry> = emptyList(),
    val query: String = "",
) {
    val isEmpty: Boolean
        get() = !isLoading && entries.isEmpty()

    val isFiltered: Boolean
        get() = query.isNotBlank()
}

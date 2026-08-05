package com.zdon.core.data.repository

import com.zdon.core.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

/** Read/write access to completed download history. */
interface HistoryRepository {

    fun observeHistory(): Flow<List<HistoryEntry>>

    fun observeHistory(query: String): Flow<List<HistoryEntry>>

    fun observeRecentUrls(limit: Int): Flow<List<String>>

    suspend fun delete(id: Long)

    suspend fun clear()
}

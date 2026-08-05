package com.zdon.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.zdon.core.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

/** Queries for the download history screen. */
@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY completed_at DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query(
        """
        SELECT * FROM history
        WHERE title LIKE '%' || :query || '%' OR uploader LIKE '%' || :query || '%'
        ORDER BY completed_at DESC
        """,
    )
    fun observeMatching(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT DISTINCT url FROM history ORDER BY completed_at DESC LIMIT :limit")
    fun observeRecentUrls(limit: Int): Flow<List<String>>

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getById(id: Long): HistoryEntity?

    @Upsert
    suspend fun upsert(entity: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAll()
}

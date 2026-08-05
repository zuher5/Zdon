package com.zdon.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.zdon.core.database.entity.DownloadEntity
import com.zdon.core.model.DownloadErrorType
import com.zdon.core.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * All download queries. Progress updates are deliberately narrow `UPDATE`
 * statements instead of full-row upserts so concurrent writes from several
 * running downloads cannot clobber each other's metadata.
 */
@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY created_at DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query(
        """
        SELECT * FROM downloads
        WHERE status IN ('QUEUED', 'RUNNING', 'PAUSED', 'FAILED')
        ORDER BY created_at ASC
        """,
    )
    fun observeActive(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'RUNNING' ORDER BY created_at ASC")
    fun observeRunning(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun observeById(id: Long): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY created_at ASC LIMIT :limit")
    suspend fun getNextQueued(limit: Int): List<DownloadEntity>

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'RUNNING'")
    suspend fun countRunning(): Int

    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('QUEUED', 'RUNNING')")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'RUNNING') ORDER BY created_at ASC")
    suspend fun getUnfinished(): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DownloadEntity): Long

    @Upsert
    suspend fun upsert(entity: DownloadEntity)

    @Query(
        """
        UPDATE downloads SET
            progress_percent = :percent,
            downloaded_bytes = :downloadedBytes,
            total_bytes = :totalBytes,
            speed_bytes_per_second = :speedBytesPerSecond,
            eta_seconds = :etaSeconds,
            current_fragment = :fragment,
            updated_at = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateProgress(
        id: Long,
        percent: Float,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSecond: Long,
        etaSeconds: Long,
        fragment: String?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE downloads SET
            status = :status,
            updated_at = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateStatus(id: Long, status: DownloadStatus, updatedAt: Long)

    @Query(
        """
        UPDATE downloads SET
            status = 'COMPLETED',
            progress_percent = 100.0,
            downloaded_bytes = :fileSizeBytes,
            total_bytes = :fileSizeBytes,
            speed_bytes_per_second = 0,
            eta_seconds = 0,
            current_fragment = NULL,
            output_path = :outputPath,
            output_file_name = :fileName,
            error_type = NULL,
            error_message = NULL,
            updated_at = :completedAt,
            completed_at = :completedAt
        WHERE id = :id
        """,
    )
    suspend fun markCompleted(
        id: Long,
        outputPath: String?,
        fileName: String?,
        fileSizeBytes: Long,
        completedAt: Long,
    )

    @Query(
        """
        UPDATE downloads SET
            status = 'FAILED',
            speed_bytes_per_second = 0,
            eta_seconds = -1,
            error_type = :errorType,
            error_message = :message,
            retry_count = :retryCount,
            updated_at = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun markFailed(
        id: Long,
        errorType: DownloadErrorType,
        message: String?,
        retryCount: Int,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE downloads SET
            status = :status,
            speed_bytes_per_second = 0,
            eta_seconds = -1,
            updated_at = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun markStopped(id: Long, status: DownloadStatus, updatedAt: Long)

    @Query(
        """
        UPDATE downloads SET
            status = 'QUEUED',
            progress_percent = 0.0,
            speed_bytes_per_second = 0,
            eta_seconds = -1,
            current_fragment = NULL,
            error_type = NULL,
            error_message = NULL,
            retry_count = :retryCount,
            updated_at = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun requeue(id: Long, retryCount: Int, updatedAt: Long)

    /**
     * Called on startup: anything left in `RUNNING` after a process death is not
     * actually running, so it is demoted to `PAUSED` and can be resumed.
     */
    @Query(
        """
        UPDATE downloads SET
            status = 'PAUSED',
            speed_bytes_per_second = 0,
            eta_seconds = -1,
            updated_at = :updatedAt
        WHERE status = 'RUNNING'
        """,
    )
    suspend fun demoteOrphanedRunning(updatedAt: Long): Int

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED', 'CANCELLED')")
    suspend fun deleteFinished()

    @Query("DELETE FROM downloads")
    suspend fun deleteAll()
}

package com.zdon.core.downloader.engine

import com.zdon.core.database.dao.DownloadDao
import com.zdon.core.database.entity.DownloadEntity
import com.zdon.core.model.DownloadErrorType
import com.zdon.core.model.DownloadProgress
import com.zdon.core.model.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressThrottleTest {

    @Test
    fun `flush writes the latest pending snapshot exactly once`() = runTest {
        val dao = FakeDownloadDao()
        val throttle = ProgressThrottle()

        throttle.offer(progress(percent = 10f, bytes = 100L))
        throttle.offer(progress(percent = 55f, bytes = 550L))
        throttle.flush(dao, now = 1_000L)

        assertEquals(1, dao.updates.size)
        val write = dao.updates.single()
        assertEquals(55f, write.percent, 0f)
        assertEquals(550L, write.downloadedBytes)
        assertEquals(1_000L, write.updatedAt)
    }

    @Test
    fun `flush with nothing pending performs no write`() = runTest {
        val dao = FakeDownloadDao()
        ProgressThrottle().flush(dao, now = 1_000L)
        assertTrue(dao.updates.isEmpty())
    }

    @Test
    fun `flush skips the second update inside the throttle window`() = runTest {
        val dao = FakeDownloadDao()
        val throttle = ProgressThrottle()

        throttle.offer(progress(10f))
        throttle.flush(dao, now = 1_000L)
        throttle.offer(progress(20f))
        // Same timestamp: inside the window, so the write is dropped.
        throttle.flush(dao, now = 1_000L)

        assertEquals(1, dao.updates.size)
    }

    @Test
    fun `flush writes again once the window has elapsed`() = runTest {
        val dao = FakeDownloadDao()
        val throttle = ProgressThrottle()

        throttle.offer(progress(10f))
        throttle.flush(dao, now = 1_000L)
        throttle.offer(progress(30f))
        // A minute later, well past any throttle window.
        throttle.flush(dao, now = 61_000L)

        assertEquals(2, dao.updates.size)
        assertEquals(30f, dao.updates.last().percent, 0f)
    }

    @Test
    fun `pending data survives both flushes while the window is skipped`() = runTest {
        val dao = FakeDownloadDao()
        val throttle = ProgressThrottle()

        throttle.offer(progress(10f))
        throttle.flush(dao, now = 1_000L)
        throttle.offer(progress(40f))
        throttle.flush(dao, now = 1_000L)
        throttle.offer(progress(90f))
        throttle.flush(dao, now = 61_000L)

        // Only the very latest value is written, older ones are coalesced.
        assertEquals(2, dao.updates.size)
        assertEquals(90f, dao.updates.last().percent, 0f)
    }

    private fun progress(percent: Float, bytes: Long = 0L): DownloadProgress =
        DownloadProgress(
            downloadId = 7,
            percent = percent,
            downloadedBytes = bytes,
            totalBytes = 0L,
            speedBytesPerSecond = 0L,
            etaSeconds = -1L,
            fragment = null,
            rawLine = "",
        )

    private class FakeDownloadDao : DownloadDao {
        var updates: MutableList<ProgressWrite> = mutableListOf()

        override suspend fun updateProgress(
            id: Long,
            percent: Float,
            downloadedBytes: Long,
            totalBytes: Long,
            speedBytesPerSecond: Long,
            etaSeconds: Long,
            fragment: String?,
            updatedAt: Long,
        ) {
            updates += ProgressWrite(
                id = id,
                percent = percent,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                speedBytesPerSecond = speedBytesPerSecond,
                etaSeconds = etaSeconds,
                fragment = fragment,
                updatedAt = updatedAt,
            )
        }

        override fun observeAll(): Flow<List<DownloadEntity>> = emptyFlow()
        override fun observeActive(): Flow<List<DownloadEntity>> = emptyFlow()
        override fun observeRunning(): Flow<List<DownloadEntity>> = emptyFlow()
        override fun observeById(id: Long): Flow<DownloadEntity?> = emptyFlow()
        override fun observeActiveCount(): Flow<Int> = emptyFlow()

        override suspend fun getById(id: Long): DownloadEntity? = null
        override suspend fun getNextQueued(limit: Int): List<DownloadEntity> = emptyList()
        override suspend fun getAllIds(): List<Long> = emptyList()
        override suspend fun countRunning(): Int = 0
        override suspend fun getUnfinished(): List<DownloadEntity> = emptyList()
        override suspend fun insert(entity: DownloadEntity): Long = 0L
        override suspend fun upsert(entity: DownloadEntity) = Unit
        override suspend fun updateStatus(id: Long, status: DownloadStatus, updatedAt: Long) = Unit
        override suspend fun markCompleted(
            id: Long, outputPath: String?, fileName: String?, fileSizeBytes: Long, completedAt: Long,
        ) = Unit

        override suspend fun markFailed(
            id: Long, errorType: DownloadErrorType, message: String?, retryCount: Int, updatedAt: Long,
        ) = Unit

        override suspend fun markStopped(id: Long, status: DownloadStatus, updatedAt: Long) = Unit
        override suspend fun requeue(id: Long, retryCount: Int, updatedAt: Long) = Unit
        override suspend fun demoteOrphanedRunning(updatedAt: Long): Int = 0
        override suspend fun requeueOrphanedRunning(updatedAt: Long): Int = 0
        override suspend fun deleteById(id: Long) = Unit
        override suspend fun deleteFinished() = Unit
        override suspend fun deleteAll() = Unit
    }

    private data class ProgressWrite(
        val id: Long,
        val percent: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speedBytesPerSecond: Long,
        val etaSeconds: Long,
        val fragment: String?,
        val updatedAt: Long,
    )
}
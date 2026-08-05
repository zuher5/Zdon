package com.zdon.core.downloader.engine

import com.zdon.core.database.dao.DownloadDao
import com.zdon.core.model.DownloadProgress
import java.util.concurrent.atomic.AtomicReference

/**
 * Coalesces yt-dlp's high-frequency progress callbacks into at most one database
 * write per [MIN_INTERVAL_MILLIS].
 *
 * yt-dlp emits a progress line for every HTTP chunk, which can exceed a hundred
 * updates per second on a fast link. Writing each one would keep the Room writer
 * thread saturated and cause visible UI jank. The latest value is always kept, so
 * throttling never loses the final state.
 */
internal class ProgressThrottle {

    private val pending = AtomicReference<DownloadProgress?>(null)
    private val lastWriteAt = AtomicReference(0L)

    /** Records [progress] and reports whether it should be written now. */
    fun offer(progress: DownloadProgress) {
        pending.set(progress)
    }

    /** Writes the latest snapshot when the throttle window has elapsed. */
    suspend fun flushIfDue(dao: DownloadDao) {
        val now = System.currentTimeMillis()
        if (now - lastWriteAt.get() < MIN_INTERVAL_MILLIS) return
        flush(dao, now)
    }

    /** Unconditionally writes the latest snapshot, if there is one. */
    suspend fun flush(dao: DownloadDao, now: Long = System.currentTimeMillis()) {
        val snapshot = pending.getAndSet(null) ?: return
        lastWriteAt.set(now)
        dao.updateProgress(
            id = snapshot.downloadId,
            percent = snapshot.percent,
            downloadedBytes = snapshot.downloadedBytes,
            totalBytes = snapshot.totalBytes,
            speedBytesPerSecond = snapshot.speedBytesPerSecond,
            etaSeconds = snapshot.etaSeconds,
            fragment = snapshot.fragment,
            updatedAt = now,
        )
    }

    /**
     * Discards the pending snapshot. Used when the caller has already written a
     * terminal state and a stale progress row would overwrite it.
     */
    fun discard() {
        pending.set(null)
    }

    companion object {
        const val MIN_INTERVAL_MILLIS = 400L
    }
}

package com.zdon.core.downloader

import com.zdon.core.model.DownloadItem
import com.zdon.core.model.DownloadRequest
import com.zdon.core.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contract the UI layer uses to drive downloads. Implemented by
 * [com.zdon.core.downloader.manager.DownloadManagerImpl]; kept as an interface so
 * ViewModels can be unit-tested with a fake.
 */
interface ZdonDownloadManager {

    /** Every download, newest first. */
    fun observeDownloads(): Flow<List<DownloadItem>>

    /** Downloads that are queued, running, paused or failed. */
    fun observeActiveDownloads(): Flow<List<DownloadItem>>

    /** A single download, or `null` once it has been deleted. */
    fun observeDownload(id: Long): Flow<DownloadItem?>

    /** Number of queued plus running downloads; drives the badge in the UI. */
    fun observeActiveCount(): Flow<Int>

    /** Enqueues [request] and returns the new row id. */
    suspend fun enqueue(request: DownloadRequest): Long

    /** Enqueues one download per element and returns the new row ids. */
    suspend fun enqueueAll(requests: List<DownloadRequest>): List<Long>

    /** Stops the process but keeps partial data so it can be resumed. */
    suspend fun pause(id: Long)

    /** Returns a paused or failed download to the queue. */
    suspend fun resume(id: Long)

    /** Stops the process and discards partial data. */
    suspend fun cancel(id: Long)

    /** Resets the retry counter and re-queues a failed or cancelled download. */
    suspend fun retry(id: Long)

    /** Cancels every active download. */
    suspend fun cancelAll()

    /** Removes a download row and its workspace. */
    suspend fun remove(id: Long)

    /** Clears completed and cancelled rows. */
    suspend fun clearFinished()

    /**
     * Re-queues anything that was running when the process died. Called once from
     * [android.app.Application.onCreate].
     */
    suspend fun recoverAfterProcessDeath()

    /** Current status of a single download without collecting a flow. */
    suspend fun statusOf(id: Long): DownloadStatus?
}

package com.zdon.core.data.repository

import com.zdon.core.model.MediaInfo
import kotlinx.coroutines.flow.Flow

/** Analyses URLs and exposes engine readiness to the UI. */
interface MediaRepository {

    /** Fetches metadata and formats for [url]. */
    suspend fun analyze(url: String, includePlaylist: Boolean): AnalyzeResult

    /** Engine readiness, including the yt-dlp version and FFmpeg availability. */
    fun observeEngineStatus(): Flow<com.zdon.core.model.EngineStatus>

    /** Prepares the bundled binaries; safe to call repeatedly. */
    suspend fun initializeEngine()

    /** Downloads the newest yt-dlp build. */
    suspend fun updateYtDlp(): com.zdon.core.model.BinaryUpdateResult

    /** Re-extracts the bundled FFmpeg payload. */
    suspend fun refreshFfmpeg(): com.zdon.core.model.BinaryUpdateResult
}

/** Outcome of [MediaRepository.analyze]. */
sealed interface AnalyzeResult {
    data class Success(val mediaInfo: MediaInfo) : AnalyzeResult
    data class Failure(
        val errorType: com.zdon.core.model.DownloadErrorType,
        val message: String,
    ) : AnalyzeResult
}

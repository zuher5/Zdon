package com.zdon.core.model

/**
 * Outcome of a single engine invocation. Distinguishing cancellation from
 * failure matters because a cancelled item must not be auto-retried.
 */
sealed interface DownloadOutcome {

    data class Success(
        val filePath: String?,
        val fileName: String?,
        val fileSizeBytes: Long,
        val elapsedMillis: Long,
    ) : DownloadOutcome

    data class Failure(
        val errorType: DownloadErrorType,
        val message: String,
    ) : DownloadOutcome

    data object Cancelled : DownloadOutcome
}

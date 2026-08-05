package com.zdon.core.model

/** Lifecycle states a download can be in. Persisted by ordinal-independent name. */
enum class DownloadStatus {
    /** Accepted by the manager and waiting for a free worker slot. */
    QUEUED,

    /** yt-dlp is currently running for this item. */
    RUNNING,

    /** Explicitly paused by the user; byte progress is preserved on disk. */
    PAUSED,

    /** Finished successfully and the output file exists. */
    COMPLETED,

    /** Terminated with an error. See [DownloadItem.errorType]. */
    FAILED,

    /** Cancelled by the user; partial files are removed. */
    CANCELLED,
    ;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == CANCELLED

    val isActive: Boolean
        get() = this == QUEUED || this == RUNNING

    val canPause: Boolean
        get() = this == RUNNING || this == QUEUED

    val canResume: Boolean
        get() = this == PAUSED || this == FAILED

    val canCancel: Boolean
        get() = this == QUEUED || this == RUNNING || this == PAUSED

    val canRetry: Boolean
        get() = this == FAILED || this == CANCELLED
}

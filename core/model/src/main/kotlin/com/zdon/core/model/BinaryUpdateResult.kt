package com.zdon.core.model

/** Result of asking the engine to refresh the bundled yt-dlp binary. */
enum class BinaryUpdateResult {
    UPDATED,
    ALREADY_UP_TO_DATE,
    FAILED,
}

package com.zdon.core.model

/**
 * Reports whether the native payloads shipped with the app are ready to use.
 * Surfaced in settings so a user can tell a missing-binary failure apart from a
 * network failure.
 */
data class EngineStatus(
    val isInitialized: Boolean,
    val ytDlpVersion: String?,
    val isFfmpegAvailable: Boolean,
    val initializationError: String?,
) {
    companion object {
        val Unknown: EngineStatus = EngineStatus(
            isInitialized = false,
            ytDlpVersion = null,
            isFfmpegAvailable = false,
            initializationError = null,
        )
    }
}

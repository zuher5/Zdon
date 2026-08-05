package com.zdon.feature.downloads

import androidx.annotation.StringRes
import com.zdon.core.model.DownloadErrorType
import com.zdon.core.model.DownloadStatus

/**
 * Maps engine enums onto string resources so the UI shows a localized,
 * actionable message instead of raw yt-dlp output.
 */
internal object DownloadStrings {

    @StringRes
    fun statusLabel(status: DownloadStatus): Int = when (status) {
        DownloadStatus.QUEUED -> R.string.downloads_status_queued
        DownloadStatus.RUNNING -> R.string.downloads_status_running
        DownloadStatus.PAUSED -> R.string.downloads_status_paused
        DownloadStatus.COMPLETED -> R.string.downloads_status_completed
        DownloadStatus.FAILED -> R.string.downloads_status_failed
        DownloadStatus.CANCELLED -> R.string.downloads_status_cancelled
    }

    @StringRes
    fun errorLabel(errorType: DownloadErrorType): Int = when (errorType) {
        DownloadErrorType.NETWORK -> R.string.error_network
        DownloadErrorType.FORBIDDEN -> R.string.error_forbidden
        DownloadErrorType.NOT_FOUND -> R.string.error_not_found
        DownloadErrorType.PRIVATE_MEDIA -> R.string.error_private
        DownloadErrorType.AGE_RESTRICTED -> R.string.error_age_restricted
        DownloadErrorType.GEO_RESTRICTED -> R.string.error_geo_restricted
        DownloadErrorType.CAPTCHA_REQUIRED -> R.string.error_captcha
        DownloadErrorType.COOKIES_EXPIRED -> R.string.error_cookies_expired
        DownloadErrorType.DISK_FULL -> R.string.error_disk_full
        DownloadErrorType.PERMISSION_DENIED -> R.string.error_permission_denied
        DownloadErrorType.BINARY_MISSING -> R.string.error_binary_missing
        DownloadErrorType.FFMPEG_MISSING -> R.string.error_ffmpeg_missing
        DownloadErrorType.FORMAT_UNAVAILABLE -> R.string.error_format_unavailable
        DownloadErrorType.UNSUPPORTED_URL -> R.string.error_unsupported_url
        DownloadErrorType.INTERRUPTED -> R.string.error_interrupted
        DownloadErrorType.UNKNOWN -> R.string.error_unknown
    }

    @StringRes
    fun filterLabel(filter: DownloadFilter): Int = when (filter) {
        DownloadFilter.ALL -> R.string.downloads_filter_all
        DownloadFilter.ACTIVE -> R.string.downloads_filter_active
        DownloadFilter.COMPLETED -> R.string.downloads_filter_completed
        DownloadFilter.FAILED -> R.string.downloads_filter_failed
    }
}

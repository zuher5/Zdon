package com.zdon.core.downloader.notification

/**
 * Notification channel, id and action constants shared by the foreground
 * service and the notification builder.
 */
object DownloadNotifications {

    /** Ongoing transfers; low importance so it never interrupts the user. */
    const val CHANNEL_PROGRESS: String = "zdon_download_progress"

    /** Terminal states; default importance so completion is noticed. */
    const val CHANNEL_STATUS: String = "zdon_download_status"

    /** Fixed id of the foreground summary notification. */
    const val FOREGROUND_NOTIFICATION_ID: Int = 1

    /** Offset applied to a download id to derive its own notification id. */
    const val COMPLETION_ID_OFFSET: Int = 10_000

    const val ACTION_PAUSE: String = "com.zdon.app.action.PAUSE"
    const val ACTION_RESUME: String = "com.zdon.app.action.RESUME"
    const val ACTION_CANCEL: String = "com.zdon.app.action.CANCEL"
    const val ACTION_RETRY: String = "com.zdon.app.action.RETRY"
    const val ACTION_STOP_ALL: String = "com.zdon.app.action.STOP_ALL"

    const val EXTRA_DOWNLOAD_ID: String = "com.zdon.app.extra.DOWNLOAD_ID"

    /** Derives a stable per-download notification id that cannot collide. */
    fun completionNotificationId(downloadId: Long): Int =
        (COMPLETION_ID_OFFSET + (downloadId % Int.MAX_VALUE)).toInt()
}

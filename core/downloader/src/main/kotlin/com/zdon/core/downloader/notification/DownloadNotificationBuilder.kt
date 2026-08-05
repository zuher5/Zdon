package com.zdon.core.downloader.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.zdon.core.downloader.R
import com.zdon.core.model.DownloadItem
import com.zdon.core.model.DownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts every notification the downloader shows.
 *
 * The service-facing methods return a [Notification] instead of posting it so the
 * foreground service can hand the same object to `startForeground`, which is
 * required for the notification to appear immediately on Android 13+.
 *
 * On Android 13+ `POST_NOTIFICATIONS` is a runtime permission; all posting paths
 * check it, so a user who declined the prompt still gets working downloads
 * without a `SecurityException`.
 */
@Singleton
class DownloadNotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    /** The summary notification that keeps the download service in foreground. */
    fun buildForegroundNotification(
        activeItems: List<DownloadItem>,
        queuedCount: Int,
        contentIntent: PendingIntent?,
    ): Notification {
        val builder = NotificationCompat.Builder(context, DownloadNotifications.CHANNEL_PROGRESS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentIntent)

        val primary = activeItems.firstOrNull()
        if (primary == null) {
            builder.setContentTitle(context.getString(R.string.notification_preparing_title))
                .setContentText(context.getString(R.string.notification_preparing_text))
                .setProgress(0, 0, true)
            return builder.build()
        }

        val extraCount = activeItems.size - 1 + queuedCount
        val title = if (extraCount > 0) {
            context.resources.getQuantityString(
                R.plurals.notification_downloading_with_others,
                extraCount,
                primary.title,
                extraCount,
            )
        } else {
            primary.title
        }

        builder.setContentTitle(title)
            .setContentText(progressSummary(primary))
            .setSubText(context.getString(R.string.notification_channel_progress_name))
            .setProgress(
                PROGRESS_MAX,
                (primary.progressFraction * PROGRESS_MAX).toInt(),
                primary.isIndeterminate,
            )

        addAction(builder, primary)
        addCancelAction(builder, primary.id)
        addStopAllAction(builder)

        return builder.build()
    }

    /** Posts a terminal-state notification for a single download. */
    fun notifyFinished(item: DownloadItem, contentIntent: PendingIntent?) {
        if (!canPostNotifications()) return

        val builder = NotificationCompat.Builder(context, DownloadNotifications.CHANNEL_STATUS)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        when (item.status) {
            DownloadStatus.COMPLETED -> builder
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(context.getString(R.string.notification_completed_title))
                .setContentText(item.title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(item.title))

            DownloadStatus.FAILED -> {
                builder
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle(context.getString(R.string.notification_failed_title))
                    .setContentText(item.errorMessage ?: item.title)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("${item.title}\n${item.errorMessage.orEmpty()}"),
                    )
                addRetryAction(builder, item.id)
            }

            else -> return
        }

        postSafely(DownloadNotifications.completionNotificationId(item.id), builder.build())
    }

    /**
     * Refreshes the already-visible foreground notification.
     *
     * The service uses this instead of posting directly so every posting path
     * goes through the same permission check and [SecurityException] guard.
     */
    fun updateForeground(notification: Notification) {
        if (!canPostNotifications()) return
        postSafely(DownloadNotifications.FOREGROUND_NOTIFICATION_ID, notification)
    }

    /** Removes the per-download terminal notification. */
    fun cancelFinished(downloadId: Long) {
        notificationManager.cancel(DownloadNotifications.completionNotificationId(downloadId))
    }

    /** True when notifications can actually be shown. */
    fun canPostNotifications(): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun progressSummary(item: DownloadItem): String {
        val parts = buildList {
            add(com.zdon.core.common.util.Formatters.formatPercent(item.progressPercent))
            com.zdon.core.common.util.Formatters.formatSpeedOrNull(item.speedBytesPerSecond)
                ?.let(::add)
            if (item.etaSeconds > 0L) {
                add(
                    context.getString(
                        R.string.notification_eta,
                        com.zdon.core.common.util.Formatters.formatDuration(item.etaSeconds),
                    ),
                )
            }
        }
        return parts.joinToString(SEPARATOR)
    }

    private fun addAction(builder: NotificationCompat.Builder, item: DownloadItem) {
        when {
            item.status == DownloadStatus.RUNNING -> builder.addAction(
                android.R.drawable.ic_media_pause,
                context.getString(R.string.action_pause),
                actionIntent(DownloadNotifications.ACTION_PAUSE, item.id),
            )

            item.status.canResume -> builder.addAction(
                android.R.drawable.ic_media_play,
                context.getString(R.string.action_resume),
                actionIntent(DownloadNotifications.ACTION_RESUME, item.id),
            )
        }
    }

    private fun addCancelAction(builder: NotificationCompat.Builder, downloadId: Long) {
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            context.getString(R.string.action_cancel),
            actionIntent(DownloadNotifications.ACTION_CANCEL, downloadId),
        )
    }

    private fun addRetryAction(builder: NotificationCompat.Builder, downloadId: Long) {
        builder.addAction(
            android.R.drawable.ic_menu_rotate,
            context.getString(R.string.action_retry),
            actionIntent(DownloadNotifications.ACTION_RETRY, downloadId),
        )
    }

    private fun addStopAllAction(builder: NotificationCompat.Builder) {
        builder.addAction(
            android.R.drawable.ic_delete,
            context.getString(R.string.action_stop_all),
            actionIntent(DownloadNotifications.ACTION_STOP_ALL, downloadId = null),
        )
    }

    /**
     * Builds a broadcast intent for a notification action.
     *
     * `FLAG_IMMUTABLE` is mandatory from Android 12 onwards; the request code is
     * derived from the action and id so actions for different downloads do not
     * overwrite each other.
     */
    private fun actionIntent(action: String, downloadId: Long?): PendingIntent {
        val intent = Intent(action).apply {
            setPackage(context.packageName)
            downloadId?.let { putExtra(DownloadNotifications.EXTRA_DOWNLOAD_ID, it) }
        }
        val requestCode = (action.hashCode() * REQUEST_CODE_PRIME + (downloadId ?: 0L).toInt())
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun postSafely(id: Int, notification: Notification) {
        try {
            notificationManager.notify(id, notification)
        } catch (exception: SecurityException) {
            Timber.w(exception, "Notification %d suppressed: permission missing", id)
        }
    }

    private fun createChannels() {
        val manager = context.getSystemService<NotificationManager>() ?: return

        val progressChannel = NotificationChannel(
            DownloadNotifications.CHANNEL_PROGRESS,
            context.getString(R.string.notification_channel_progress_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_progress_description)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val statusChannel = NotificationChannel(
            DownloadNotifications.CHANNEL_STATUS,
            context.getString(R.string.notification_channel_status_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_status_description)
            setShowBadge(true)
        }

        manager.createNotificationChannels(listOf(progressChannel, statusChannel))
    }

    private companion object {
        const val PROGRESS_MAX = 100
        const val SEPARATOR = " · "
        const val REQUEST_CODE_PRIME = 31
    }
}

package com.zdon.core.downloader.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.zdon.core.downloader.manager.DownloadManagerImpl
import com.zdon.core.downloader.notification.DownloadNotificationBuilder
import com.zdon.core.downloader.notification.DownloadNotifications
import com.zdon.core.model.DownloadItem
import com.zdon.core.model.DownloadStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import javax.inject.Inject

/**
 * Keeps the process alive while downloads run.
 *
 * The service does not perform the transfer itself: [DownloadManagerImpl] owns the
 * coroutines. The service exists so Android does not kill the process, and so the
 * user always sees an accurate persistent notification. It stops itself as soon as
 * the queue drains, which avoids the battery cost of an idle foreground service.
 *
 * `startForeground` is called from `onStartCommand` before any suspending work, as
 * required on Android 8+ and enforced strictly on Android 12+.
 */
@AndroidEntryPoint
class DownloadService : Service() {

    @Inject
    lateinit var downloadManager: DownloadManagerImpl

    @Inject
    lateinit var notificationBuilder: DownloadNotificationBuilder

    private val serviceJob: Job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var isForeground = false

    override fun onCreate() {
        super.onCreate()
        observeQueue()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promoteToForeground(
            notificationBuilder.buildForegroundNotification(
                activeItems = emptyList(),
                queuedCount = 0,
                contentIntent = launchAppIntent(),
            ),
        )
        serviceScope.launch { downloadManager.pump() }
        // Redeliver so a killed service resumes the queue on restart.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeQueue() {
        serviceScope.launch {
            downloadManager.observeActiveDownloads()
                .distinctUntilChanged()
                .collectLatest { items -> onQueueChanged(items) }
        }
    }

    private fun onQueueChanged(items: List<DownloadItem>) {
        val running = items.filter { it.status == DownloadStatus.RUNNING }
        val queued = items.count { it.status == DownloadStatus.QUEUED }

        if (running.isEmpty() && queued == 0) {
            stopSelfSafely()
            return
        }

        val notification = notificationBuilder.buildForegroundNotification(
            activeItems = running,
            queuedCount = queued,
            contentIntent = launchAppIntent(),
        )
        if (isForeground) {
            notificationBuilder.updateForeground(notification)
        } else {
            promoteToForeground(notification)
        }
    }

    private fun promoteToForeground(notification: Notification) {
        if (isForeground) return
        try {
            ServiceCompat.startForeground(
                this,
                DownloadNotifications.FOREGROUND_NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                },
            )
            isForeground = true
        } catch (exception: IllegalStateException) {
            // Android 12+ throws when a background start is not allowed. The
            // downloads still run; only the persistent notification is skipped.
            Timber.w(exception, "Unable to enter the foreground")
        } catch (exception: SecurityException) {
            Timber.w(exception, "Foreground service type not permitted")
        }
    }

    private fun stopSelfSafely() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
    }

    private fun launchAppIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return null
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            this,
            LAUNCH_REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val LAUNCH_REQUEST_CODE = 1001

        /**
         * Starts the service if it is not already running. Safe to call from a
         * ViewModel: on Android 12+ a foreground start from the background throws,
         * which is caught and logged rather than crashing the app.
         */
        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (exception: IllegalStateException) {
                Timber.w(exception, "Background start not allowed; queue will run in-process")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DownloadService::class.java))
        }
    }
}

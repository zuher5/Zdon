package com.zdon.core.downloader.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zdon.core.common.di.ApplicationScope
import com.zdon.core.downloader.ZdonDownloadManager
import com.zdon.core.downloader.notification.DownloadNotifications
import com.zdon.core.downloader.service.DownloadService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Handles the pause/resume/cancel/retry actions attached to the download
 * notification.
 *
 * A receiver's `onReceive` runs on the main thread and must return quickly, so the
 * work is dispatched to the injected application scope rather than done inline.
 */
@AndroidEntryPoint
class DownloadActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var downloadManager: ZdonDownloadManager

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val downloadId = intent.getLongExtra(DownloadNotifications.EXTRA_DOWNLOAD_ID, INVALID_ID)

        if (action != DownloadNotifications.ACTION_STOP_ALL && downloadId == INVALID_ID) {
            Timber.w("Notification action %s received without a download id", action)
            return
        }

        val pendingResult = goAsync()
        scope.launch {
            try {
                when (action) {
                    DownloadNotifications.ACTION_PAUSE -> downloadManager.pause(downloadId)
                    DownloadNotifications.ACTION_RESUME -> {
                        downloadManager.resume(downloadId)
                        DownloadService.start(context)
                    }
                    DownloadNotifications.ACTION_CANCEL -> downloadManager.cancel(downloadId)
                    DownloadNotifications.ACTION_RETRY -> {
                        downloadManager.retry(downloadId)
                        DownloadService.start(context)
                    }
                    DownloadNotifications.ACTION_STOP_ALL -> downloadManager.cancelAll()
                    else -> Timber.w("Unhandled notification action %s", action)
                }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Notification action %s failed", action)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val INVALID_ID = -1L
    }
}

package com.zdon.core.downloader.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zdon.core.common.di.ApplicationScope
import com.zdon.core.downloader.manager.DownloadManagerImpl
import com.zdon.core.downloader.service.DownloadService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Resumes interrupted downloads after a reboot.
 *
 * Downloads that were running when the device shut down are demoted to `PAUSED`
 * by [DownloadManagerImpl.recoverAfterProcessDeath], so this receiver only hands
 * control back to the queue and starts the service when work remains.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var downloadManager: DownloadManagerImpl

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        val pendingResult = goAsync()
        scope.launch {
            try {
                downloadManager.recoverAfterProcessDeath()
                if (downloadManager.hasWork()) {
                    DownloadService.start(context)
                }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Boot recovery failed")
            } finally {
                pendingResult.finish()
            }
        }
    }
}

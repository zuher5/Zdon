package com.zdon.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.zdon.core.common.di.ApplicationScope
import com.zdon.core.data.repository.DownloadRepository
import com.zdon.core.downloader.worker.BinaryUpdateWorker
import com.zdon.app.logging.CrashSafeReleaseTree
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Work done here is deliberately minimal and non-blocking: heavy initialisation
 * (unpacking the Python runtime) happens lazily inside the engine, and queue
 * recovery runs on the injected application scope so `onCreate` never blocks the
 * main thread and cannot cause a startup ANR.
 */
@HiltAndroidApp
class ZdonApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.WARN)
            .build()

    override fun onCreate() {
        super.onCreate()
        installLogging()
        recoverDownloads()
        scheduleBinaryUpdates()
    }

    private fun installLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashSafeReleaseTree())
        }
    }

    private fun recoverDownloads() {
        applicationScope.launch {
            try {
                downloadRepository.recoverAfterProcessDeath()
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Download recovery failed")
            }
        }
    }

    private fun scheduleBinaryUpdates() {
        applicationScope.launch {
            try {
                BinaryUpdateWorker.schedule(WorkManager.getInstance(this@ZdonApplication))
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Unable to schedule binary updates")
            }
        }
    }
}

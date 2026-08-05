package com.zdon.core.downloader.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zdon.core.datastore.UserPreferencesDataSource
import com.zdon.core.engine.YtDlpInitializer
import com.zdon.core.model.BinaryUpdateResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Refreshes the bundled yt-dlp binary in the background.
 *
 * Extractors break whenever a site changes, so keeping yt-dlp current is what
 * keeps the app working. WorkManager is used because the update must survive
 * process death and respect Doze; it runs unmetered-only so it never costs the
 * user mobile data.
 */
@HiltWorker
class BinaryUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val initializer: YtDlpInitializer,
    private val preferences: UserPreferencesDataSource,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val settings = preferences.currentPreferences()
        if (!settings.autoUpdateYtDlp && !settings.autoUpdateFfmpeg) return Result.success()

        val status = initializer.ensureInitialized()
        if (!status.isInitialized) {
            Timber.w("Skipping binary update: engine not initialised")
            return Result.retry()
        }

        var failed = false

        if (settings.autoUpdateYtDlp) {
            when (initializer.updateYtDlp()) {
                BinaryUpdateResult.UPDATED -> Timber.i("yt-dlp updated")
                BinaryUpdateResult.ALREADY_UP_TO_DATE -> Timber.d("yt-dlp already current")
                BinaryUpdateResult.FAILED -> failed = true
            }
        }

        if (settings.autoUpdateFfmpeg && initializer.refreshFfmpeg() == BinaryUpdateResult.FAILED) {
            failed = true
        }

        return if (failed) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "zdon_binary_update"
        private const val REPEAT_INTERVAL_HOURS = 24L

        /** Schedules the periodic update, keeping any existing schedule. */
        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<BinaryUpdateWorker>(
                REPEAT_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}

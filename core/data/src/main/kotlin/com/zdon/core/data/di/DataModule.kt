package com.zdon.core.data.di

import android.content.Context
import com.zdon.core.data.repository.DownloadRepository
import com.zdon.core.data.repository.DownloadRepositoryImpl
import com.zdon.core.data.repository.DownloadServiceStarter
import com.zdon.core.data.repository.HistoryRepository
import com.zdon.core.data.repository.HistoryRepositoryImpl
import com.zdon.core.data.repository.MediaRepository
import com.zdon.core.data.repository.MediaRepositoryImpl
import com.zdon.core.data.repository.SettingsRepository
import com.zdon.core.data.repository.SettingsRepositoryImpl
import com.zdon.core.downloader.ZdonDownloadManager
import com.zdon.core.downloader.service.DownloadService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds repository interfaces to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
interface DataBindingsModule {

    @Binds
    @Singleton
    fun bindsMediaRepository(implementation: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    fun bindsHistoryRepository(implementation: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    fun bindsSettingsRepository(implementation: SettingsRepositoryImpl): SettingsRepository
}

/**
 * Provides the download repository together with the service starter it needs.
 *
 * Starting the foreground service from the repository (rather than from every
 * ViewModel) guarantees the service is running whenever work is enqueued, no
 * matter which screen triggered it.
 */
@Module
@InstallIn(SingletonComponent::class)
object DownloadRepositoryModule {

    @Provides
    @Singleton
    fun providesDownloadServiceStarter(
        @ApplicationContext context: Context,
    ): DownloadServiceStarter = object : DownloadServiceStarter {
        override fun start() = DownloadService.start(context)
    }

    @Provides
    @Singleton
    fun providesDownloadRepository(
        downloadManager: ZdonDownloadManager,
        serviceStarter: DownloadServiceStarter,
    ): DownloadRepository = DownloadRepositoryImpl(downloadManager, serviceStarter)
}

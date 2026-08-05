package com.zdon.core.downloader.di

import com.zdon.core.downloader.ZdonDownloadManager
import com.zdon.core.downloader.manager.DownloadManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the download manager implementation to its public contract. */
@Module
@InstallIn(SingletonComponent::class)
interface DownloaderModule {

    @Binds
    @Singleton
    fun bindsDownloadManager(implementation: DownloadManagerImpl): ZdonDownloadManager
}

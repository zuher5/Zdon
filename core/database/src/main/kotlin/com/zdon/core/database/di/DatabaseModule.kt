package com.zdon.core.database.di

import android.content.Context
import androidx.room.Room
import com.zdon.core.database.ZdonDatabase
import com.zdon.core.database.dao.DownloadDao
import com.zdon.core.database.dao.HistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the Room database and its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesZdonDatabase(
        @ApplicationContext context: Context,
    ): ZdonDatabase = Room.databaseBuilder(
        context = context,
        klass = ZdonDatabase::class.java,
        name = ZdonDatabase.NAME,
    )
        // No destructive fallback: schema changes must ship a real migration so a
        // user never loses queued downloads or history on update.
        .build()

    @Provides
    fun providesDownloadDao(database: ZdonDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun providesHistoryDao(database: ZdonDatabase): HistoryDao = database.historyDao()
}

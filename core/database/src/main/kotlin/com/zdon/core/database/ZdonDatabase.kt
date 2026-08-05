package com.zdon.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zdon.core.database.converter.EnumConverters
import com.zdon.core.database.dao.DownloadDao
import com.zdon.core.database.dao.HistoryDao
import com.zdon.core.database.entity.DownloadEntity
import com.zdon.core.database.entity.HistoryEntity

/**
 * Room database for the download queue and the history.
 *
 * Schemas are exported to `core/database/schemas` so future versions can be
 * migrated with an auto-migration or a hand-written [androidx.room.migration.Migration]
 * instead of a destructive fallback.
 */
@Database(
    entities = [DownloadEntity::class, HistoryEntity::class],
    version = ZdonDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(EnumConverters::class)
abstract class ZdonDatabase : RoomDatabase() {

    abstract fun downloadDao(): DownloadDao

    abstract fun historyDao(): HistoryDao

    companion object {
        const val VERSION: Int = 1
        const val NAME: String = "zdon.db"
    }
}

package com.zdon.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A completed download retained for the history screen. */
@Entity(
    tableName = "history",
    indices = [Index(value = ["completed_at"]), Index(value = ["url"])],
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "uploader")
    val uploader: String? = null,

    @ColumnInfo(name = "thumbnail_url")
    val thumbnailUrl: String? = null,

    @ColumnInfo(name = "file_path")
    val filePath: String? = null,

    @ColumnInfo(name = "file_name")
    val fileName: String? = null,

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long = 0L,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long = 0L,

    @ColumnInfo(name = "extractor")
    val extractor: String? = null,

    @ColumnInfo(name = "was_audio_only")
    val wasAudioOnly: Boolean = false,

    @ColumnInfo(name = "completed_at")
    val completedAtMillis: Long,
)

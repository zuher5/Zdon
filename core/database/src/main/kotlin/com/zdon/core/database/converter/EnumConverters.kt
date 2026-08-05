package com.zdon.core.database.converter

import androidx.room.TypeConverter
import com.zdon.core.model.AudioFormat
import com.zdon.core.model.DownloadErrorType
import com.zdon.core.model.DownloadStatus
import com.zdon.core.model.VideoQuality

/**
 * Enums are stored by name rather than ordinal so reordering an enum can never
 * silently corrupt existing rows. Unknown names decay to a safe default, which
 * keeps a downgrade from crashing the app.
 */
class EnumConverters {

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus =
        DownloadStatus.entries.firstOrNull { it.name == value } ?: DownloadStatus.QUEUED

    @TypeConverter
    fun fromVideoQuality(value: VideoQuality): String = value.name

    @TypeConverter
    fun toVideoQuality(value: String): VideoQuality = VideoQuality.fromNameOrDefault(value)

    @TypeConverter
    fun fromAudioFormat(value: AudioFormat): String = value.name

    @TypeConverter
    fun toAudioFormat(value: String): AudioFormat = AudioFormat.fromNameOrDefault(value)

    @TypeConverter
    fun fromDownloadErrorType(value: DownloadErrorType?): String? = value?.name

    @TypeConverter
    fun toDownloadErrorType(value: String?): DownloadErrorType? =
        value?.let { name -> DownloadErrorType.entries.firstOrNull { it.name == name } }
}

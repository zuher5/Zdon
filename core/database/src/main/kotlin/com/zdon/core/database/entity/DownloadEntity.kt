package com.zdon.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zdon.core.model.AudioFormat
import com.zdon.core.model.DownloadErrorType
import com.zdon.core.model.DownloadStatus
import com.zdon.core.model.VideoQuality

/**
 * Persisted representation of a queued, running or finished download.
 *
 * Progress fields are updated in place; the manager throttles writes so a fast
 * download does not saturate the database. Everything needed to rebuild the
 * yt-dlp command is stored so the queue survives process death.
 */
@Entity(
    tableName = "downloads",
    indices = [
        Index(value = ["status"]),
        Index(value = ["created_at"]),
        Index(value = ["url", "format_id"]),
    ],
)
data class DownloadEntity(
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

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long = 0L,

    @ColumnInfo(name = "status")
    val status: DownloadStatus = DownloadStatus.QUEUED,

    @ColumnInfo(name = "progress_percent")
    val progressPercent: Float = 0f,

    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long = 0L,

    @ColumnInfo(name = "total_bytes")
    val totalBytes: Long = 0L,

    @ColumnInfo(name = "speed_bytes_per_second")
    val speedBytesPerSecond: Long = 0L,

    @ColumnInfo(name = "eta_seconds")
    val etaSeconds: Long = -1L,

    @ColumnInfo(name = "current_fragment")
    val currentFragment: String? = null,

    @ColumnInfo(name = "output_path")
    val outputPath: String? = null,

    @ColumnInfo(name = "output_file_name")
    val outputFileName: String? = null,

    @ColumnInfo(name = "format_id")
    val formatId: String? = null,

    @ColumnInfo(name = "quality")
    val quality: VideoQuality = VideoQuality.BEST,

    @ColumnInfo(name = "audio_format")
    val audioFormat: AudioFormat = AudioFormat.MP3,

    @ColumnInfo(name = "extract_audio")
    val extractAudio: Boolean = false,

    @ColumnInfo(name = "download_subtitles")
    val downloadSubtitles: Boolean = false,

    @ColumnInfo(name = "subtitle_languages")
    val subtitleLanguages: String? = null,

    @ColumnInfo(name = "embed_subtitles")
    val embedSubtitles: Boolean = false,

    @ColumnInfo(name = "download_thumbnail")
    val downloadThumbnail: Boolean = false,

    @ColumnInfo(name = "embed_thumbnail")
    val embedThumbnail: Boolean = false,

    @ColumnInfo(name = "embed_metadata")
    val embedMetadata: Boolean = false,

    @ColumnInfo(name = "is_playlist")
    val isPlaylist: Boolean = false,

    @ColumnInfo(name = "playlist_items")
    val playlistItems: String? = null,

    @ColumnInfo(name = "playlist_index")
    val playlistIndex: Int = 0,

    @ColumnInfo(name = "playlist_count")
    val playlistCount: Int = 0,

    @ColumnInfo(name = "output_template")
    val outputTemplate: String? = null,

    @ColumnInfo(name = "error_type")
    val errorType: DownloadErrorType? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAtMillis: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAtMillis: Long,

    @ColumnInfo(name = "completed_at")
    val completedAtMillis: Long? = null,
)

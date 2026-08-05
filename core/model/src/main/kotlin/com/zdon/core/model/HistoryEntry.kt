package com.zdon.core.model

/** One row of the download history list. */
data class HistoryEntry(
    val id: Long,
    val url: String,
    val title: String,
    val uploader: String?,
    val thumbnailUrl: String?,
    val filePath: String?,
    val fileName: String?,
    val fileSizeBytes: Long,
    val durationSeconds: Long,
    val extractor: String?,
    val wasAudioOnly: Boolean,
    val completedAtMillis: Long,
)

package com.zdon.core.downloader.storage

/** Maps file extensions to MIME types for SAF document creation. */
internal object MimeTypes {

    private const val FALLBACK = "application/octet-stream"

    private val BY_EXTENSION = mapOf(
        "mp4" to "video/mp4",
        "m4v" to "video/mp4",
        "mkv" to "video/x-matroska",
        "webm" to "video/webm",
        "mov" to "video/quicktime",
        "avi" to "video/x-msvideo",
        "flv" to "video/x-flv",
        "ts" to "video/mp2t",
        "3gp" to "video/3gpp",
        "mp3" to "audio/mpeg",
        "m4a" to "audio/mp4",
        "aac" to "audio/aac",
        "opus" to "audio/opus",
        "ogg" to "audio/ogg",
        "flac" to "audio/flac",
        "wav" to "audio/wav",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "webp" to "image/webp",
        "vtt" to "text/vtt",
        "srt" to "application/x-subrip",
        "ass" to "text/plain",
        "json" to "application/json",
        "txt" to "text/plain",
        "description" to "text/plain",
    )

    fun fromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return BY_EXTENSION[extension] ?: FALLBACK
    }
}

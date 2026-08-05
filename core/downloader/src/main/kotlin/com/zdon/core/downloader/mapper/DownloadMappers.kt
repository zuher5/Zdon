package com.zdon.core.downloader.mapper

import com.zdon.core.database.entity.DownloadEntity
import com.zdon.core.database.entity.HistoryEntity
import com.zdon.core.model.DownloadItem
import com.zdon.core.model.DownloadRequest
import com.zdon.core.model.HistoryEntry

/** Entity to domain mappers for the downloader module. */

private const val LANGUAGE_SEPARATOR = ","

fun DownloadEntity.toDomain(): DownloadItem = DownloadItem(
    id = id,
    url = url,
    title = title,
    uploader = uploader,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    status = status,
    progressPercent = progressPercent,
    downloadedBytes = downloadedBytes,
    totalBytes = totalBytes,
    speedBytesPerSecond = speedBytesPerSecond,
    etaSeconds = etaSeconds,
    currentFragment = currentFragment,
    outputPath = outputPath,
    outputFileName = outputFileName,
    formatId = formatId,
    quality = quality,
    audioFormat = audioFormat,
    extractAudio = extractAudio,
    isPlaylist = isPlaylist,
    playlistIndex = playlistIndex,
    playlistCount = playlistCount,
    errorType = errorType,
    errorMessage = errorMessage,
    retryCount = retryCount,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    completedAtMillis = completedAtMillis,
    request = toRequest(),
)

fun DownloadEntity.toRequest(): DownloadRequest = DownloadRequest(
    url = url,
    title = title,
    quality = quality,
    audioFormat = audioFormat,
    customFormatId = formatId,
    extractAudio = extractAudio,
    downloadSubtitles = downloadSubtitles,
    subtitleLanguages = subtitleLanguages
        ?.split(LANGUAGE_SEPARATOR)
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        .orEmpty(),
    embedSubtitles = embedSubtitles,
    downloadThumbnail = downloadThumbnail,
    embedThumbnail = embedThumbnail,
    embedMetadata = embedMetadata,
    isPlaylist = isPlaylist,
    playlistItems = playlistItems,
    outputTemplate = outputTemplate,
    thumbnailUrl = thumbnailUrl,
    uploader = uploader,
    durationSeconds = durationSeconds,
)

fun DownloadRequest.toEntity(nowMillis: Long): DownloadEntity = DownloadEntity(
    url = url,
    title = title,
    uploader = uploader,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    formatId = customFormatId,
    quality = quality,
    audioFormat = audioFormat,
    extractAudio = extractAudio,
    downloadSubtitles = downloadSubtitles,
    subtitleLanguages = subtitleLanguages.takeIf { it.isNotEmpty() }
        ?.joinToString(LANGUAGE_SEPARATOR),
    embedSubtitles = embedSubtitles,
    downloadThumbnail = downloadThumbnail,
    embedThumbnail = embedThumbnail,
    embedMetadata = embedMetadata,
    isPlaylist = isPlaylist,
    playlistItems = playlistItems,
    outputTemplate = outputTemplate,
    createdAtMillis = nowMillis,
    updatedAtMillis = nowMillis,
)

fun HistoryEntity.toDomain(): HistoryEntry = HistoryEntry(
    id = id,
    url = url,
    title = title,
    uploader = uploader,
    thumbnailUrl = thumbnailUrl,
    filePath = filePath,
    fileName = fileName,
    fileSizeBytes = fileSizeBytes,
    durationSeconds = durationSeconds,
    extractor = extractor,
    wasAudioOnly = wasAudioOnly,
    completedAtMillis = completedAtMillis,
)

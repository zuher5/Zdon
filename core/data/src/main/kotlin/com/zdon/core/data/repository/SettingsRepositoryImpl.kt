package com.zdon.core.data.repository

import android.net.Uri
import com.zdon.core.common.di.Dispatcher
import com.zdon.core.common.di.ZdonDispatcher
import com.zdon.core.datastore.UserPreferencesDataSource
import com.zdon.core.downloader.storage.DownloadStorageManager
import com.zdon.core.model.AudioFormat
import com.zdon.core.model.ThemeMode
import com.zdon.core.model.UserPreferences
import com.zdon.core.model.VideoQuality
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed [SettingsRepository].
 *
 * Choosing a download folder is the one setting with a side effect beyond
 * persistence: the SAF grant must be taken (and the previous one released) before
 * the value is stored, otherwise the app would remember a folder it cannot write
 * to.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
    private val storageManager: DownloadStorageManager,
    @Dispatcher(ZdonDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : SettingsRepository {

    override val userPreferences: Flow<UserPreferences> = dataSource.userPreferences

    override suspend fun current(): UserPreferences = dataSource.currentPreferences()

    override suspend fun setThemeMode(themeMode: ThemeMode) = dataSource.setThemeMode(themeMode)

    override suspend fun setUseDynamicColor(enabled: Boolean) =
        dataSource.setUseDynamicColor(enabled)

    override suspend fun setDownloadLocation(treeUri: String): Boolean =
        withContext(ioDispatcher) {
            val uri = runCatching { Uri.parse(treeUri) }.getOrNull()
                ?: return@withContext false

            if (!storageManager.persistTreePermission(uri)) {
                Timber.w("Could not persist access to %s", treeUri)
                return@withContext false
            }

            val previous = dataSource.currentPreferences().downloadTreeUri
            if (!previous.isNullOrBlank() && previous != treeUri) {
                runCatching { Uri.parse(previous) }.getOrNull()
                    ?.let(storageManager::releaseTreePermission)
            }

            dataSource.setDownloadLocation(
                treeUri = treeUri,
                label = storageManager.displayName(treeUri),
            )
            true
        }

    override suspend fun setMaxConcurrentDownloads(value: Int) =
        dataSource.setMaxConcurrentDownloads(value)

    override suspend fun setMaxRetries(value: Int) = dataSource.setMaxRetries(value)

    override suspend fun setAutoUpdateYtDlp(enabled: Boolean) =
        dataSource.setAutoUpdateYtDlp(enabled)

    override suspend fun setAutoUpdateFfmpeg(enabled: Boolean) =
        dataSource.setAutoUpdateFfmpeg(enabled)

    override suspend fun setDefaultQuality(quality: VideoQuality) =
        dataSource.setDefaultQuality(quality)

    override suspend fun setDefaultAudioFormat(format: AudioFormat) =
        dataSource.setDefaultAudioFormat(format)

    override suspend fun setNotificationsEnabled(enabled: Boolean) =
        dataSource.setNotificationsEnabled(enabled)

    override suspend fun setCookiesFileUri(uri: String?) = dataSource.setCookiesFileUri(uri)

    override suspend fun setProxyUrl(proxy: String?) = dataSource.setProxyUrl(proxy)

    override suspend fun setCustomHttpHeaders(headers: String?) =
        dataSource.setCustomHttpHeaders(headers)

    override suspend fun setOutputTemplate(template: String) =
        dataSource.setOutputTemplate(template)

    override suspend fun setEmbedMetadata(enabled: Boolean) = dataSource.setEmbedMetadata(enabled)

    override suspend fun setEmbedThumbnail(enabled: Boolean) = dataSource.setEmbedThumbnail(enabled)

    override suspend fun setDownloadSubtitles(enabled: Boolean) =
        dataSource.setDownloadSubtitles(enabled)

    override suspend fun setSubtitleLanguages(languages: String) =
        dataSource.setSubtitleLanguages(languages)

    override suspend fun setUseDownloadArchive(enabled: Boolean) =
        dataSource.setUseDownloadArchive(enabled)

    override suspend fun setRestrictFilenames(enabled: Boolean) =
        dataSource.setRestrictFilenames(enabled)

    override suspend fun setAutoResumeAfterBoot(enabled: Boolean) =
        dataSource.setAutoResumeAfterBoot(enabled)

    override suspend fun clearRecentUrls() = dataSource.clearRecentUrls()

    override suspend fun hasWritableDownloadLocation(): Boolean = withContext(ioDispatcher) {
        storageManager.canWriteTo(dataSource.currentPreferences().downloadTreeUri)
    }
}

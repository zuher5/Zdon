package com.zdon.core.data.repository

import com.zdon.core.model.AudioFormat
import com.zdon.core.model.ThemeMode
import com.zdon.core.model.UserPreferences
import com.zdon.core.model.VideoQuality
import kotlinx.coroutines.flow.Flow

/** Read/write access to user settings. */
interface SettingsRepository {

    val userPreferences: Flow<UserPreferences>

    suspend fun current(): UserPreferences

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setUseDynamicColor(enabled: Boolean)

    /**
     * Persists the chosen SAF folder. Returns `false` when the grant could not be
     * persisted, in which case the previous folder is kept.
     */
    suspend fun setDownloadLocation(treeUri: String): Boolean

    suspend fun setMaxConcurrentDownloads(value: Int)

    suspend fun setMaxRetries(value: Int)

    suspend fun setAutoUpdateYtDlp(enabled: Boolean)

    suspend fun setAutoUpdateFfmpeg(enabled: Boolean)

    suspend fun setDefaultQuality(quality: VideoQuality)

    suspend fun setDefaultAudioFormat(format: AudioFormat)

    suspend fun setNotificationsEnabled(enabled: Boolean)

    suspend fun setCookiesFileUri(uri: String?)

    suspend fun setProxyUrl(proxy: String?)

    suspend fun setCustomHttpHeaders(headers: String?)

    suspend fun setOutputTemplate(template: String)

    suspend fun setEmbedMetadata(enabled: Boolean)

    suspend fun setEmbedThumbnail(enabled: Boolean)

    suspend fun setDownloadSubtitles(enabled: Boolean)

    suspend fun setSubtitleLanguages(languages: String)

    suspend fun setUseDownloadArchive(enabled: Boolean)

    suspend fun setRestrictFilenames(enabled: Boolean)

    suspend fun setAutoResumeAfterBoot(enabled: Boolean)

    suspend fun clearRecentUrls()

    /** True when the app still holds a writable grant for the saved folder. */
    suspend fun hasWritableDownloadLocation(): Boolean
}

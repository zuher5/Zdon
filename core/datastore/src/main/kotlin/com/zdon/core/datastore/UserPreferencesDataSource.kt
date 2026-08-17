package com.zdon.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zdon.core.model.AudioFormat
import com.zdon.core.model.ThemeMode
import com.zdon.core.model.UserPreferences
import com.zdon.core.model.VideoQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for user settings.
 *
 * Reads are exposed as a [Flow] so the UI updates immediately after a write, and
 * an [IOException] while reading degrades to defaults rather than crashing the
 * collector.
 */
@Singleton
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                Timber.e(throwable, "Failed to read preferences; falling back to defaults")
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences -> preferences.toUserPreferences() }

    /** One-shot read for callers that cannot collect, e.g. Workers. */
    suspend fun currentPreferences(): UserPreferences = userPreferences.first()

    suspend fun setThemeMode(themeMode: ThemeMode) = edit {
        it[Keys.THEME_MODE] = themeMode.name
    }

    suspend fun setUseDynamicColor(enabled: Boolean) = edit {
        it[Keys.DYNAMIC_COLOR] = enabled
    }

    suspend fun setDownloadLocation(treeUri: String?, label: String?) = edit { preferences ->
        if (treeUri.isNullOrBlank()) {
            preferences.remove(Keys.DOWNLOAD_TREE_URI)
            preferences.remove(Keys.DOWNLOAD_DIRECTORY_LABEL)
        } else {
            preferences[Keys.DOWNLOAD_TREE_URI] = treeUri
            preferences[Keys.DOWNLOAD_DIRECTORY_LABEL] = label.orEmpty()
        }
    }

    suspend fun setMaxConcurrentDownloads(value: Int) = edit {
        it[Keys.MAX_CONCURRENT] = value.coerceIn(
            UserPreferences.MIN_CONCURRENT_DOWNLOADS,
            UserPreferences.MAX_CONCURRENT_DOWNLOADS,
        )
    }

    suspend fun setMaxRetries(value: Int) = edit {
        it[Keys.MAX_RETRIES] = value.coerceIn(
            UserPreferences.MIN_MAX_RETRIES,
            UserPreferences.MAX_MAX_RETRIES,
        )
    }

    suspend fun setAutoUpdateYtDlp(enabled: Boolean) = edit {
        it[Keys.AUTO_UPDATE_YTDLP] = enabled
    }

    suspend fun setAutoUpdateFfmpeg(enabled: Boolean) = edit {
        it[Keys.AUTO_UPDATE_FFMPEG] = enabled
    }

    suspend fun setDefaultQuality(quality: VideoQuality) = edit {
        it[Keys.DEFAULT_QUALITY] = quality.name
    }

    suspend fun setDefaultAudioFormat(format: AudioFormat) = edit {
        it[Keys.DEFAULT_AUDIO_FORMAT] = format.name
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) = edit {
        it[Keys.NOTIFICATIONS_ENABLED] = enabled
    }

    suspend fun setCookiesFileUri(uri: String?) = edit { preferences ->
        if (uri.isNullOrBlank()) preferences.remove(Keys.COOKIES_URI) else preferences[Keys.COOKIES_URI] = uri
    }

    suspend fun setProxyUrl(proxy: String?) = edit { preferences ->
        if (proxy.isNullOrBlank()) preferences.remove(Keys.PROXY_URL) else preferences[Keys.PROXY_URL] = proxy
    }

    suspend fun setCustomHttpHeaders(headers: String?) = edit { preferences ->
        if (headers.isNullOrBlank()) {
            preferences.remove(Keys.CUSTOM_HEADERS)
        } else {
            preferences[Keys.CUSTOM_HEADERS] = headers
        }
    }

    suspend fun setOutputTemplate(template: String) = edit {
        it[Keys.OUTPUT_TEMPLATE] = template.ifBlank { UserPreferences.DEFAULT_OUTPUT_TEMPLATE }
    }

    suspend fun setEmbedMetadata(enabled: Boolean) = edit {
        it[Keys.EMBED_METADATA] = enabled
    }

    suspend fun setEmbedThumbnail(enabled: Boolean) = edit {
        it[Keys.EMBED_THUMBNAIL] = enabled
    }

    suspend fun setDownloadSubtitles(enabled: Boolean) = edit {
        it[Keys.DOWNLOAD_SUBTITLES] = enabled
    }

    suspend fun setSubtitleLanguages(languages: String) = edit {
        it[Keys.SUBTITLE_LANGUAGES] = languages.ifBlank {
            UserPreferences.DEFAULT_SUBTITLE_LANGUAGES
        }
    }

    suspend fun setUseDownloadArchive(enabled: Boolean) = edit {
        it[Keys.USE_DOWNLOAD_ARCHIVE] = enabled
    }

    /** When enabled, interrupted downloads are queued again after a reboot. */
    suspend fun setAutoResumeAfterBoot(enabled: Boolean) = edit {
        it[Keys.AUTO_RESUME_AFTER_BOOT] = enabled
    }

    suspend fun setRestrictFilenames(enabled: Boolean) = edit {
        it[Keys.RESTRICT_FILENAMES] = enabled
    }

    /** Prepends [url] to the recent list, de-duplicating and capping the size. */
    suspend fun addRecentUrl(url: String) = edit { preferences ->
        val existing = preferences[Keys.RECENT_URLS].orEmpty().splitRecents()
        val updated = (listOf(url) + existing)
            .distinct()
            .take(UserPreferences.MAX_RECENT_URLS)
        preferences[Keys.RECENT_URLS] = updated.joinToString(RECENT_SEPARATOR)
    }

    suspend fun clearRecentUrls() = edit { it.remove(Keys.RECENT_URLS) }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        try {
            dataStore.edit(block)
        } catch (exception: IOException) {
            Timber.e(exception, "Failed to persist preference change")
        }
    }

    private fun Preferences.toUserPreferences(): UserPreferences = UserPreferences(
        themeMode = ThemeMode.fromNameOrDefault(this[Keys.THEME_MODE]),
        useDynamicColor = this[Keys.DYNAMIC_COLOR] ?: true,
        downloadTreeUri = this[Keys.DOWNLOAD_TREE_URI],
        downloadDirectoryLabel = this[Keys.DOWNLOAD_DIRECTORY_LABEL]?.takeIf { it.isNotBlank() },
        maxConcurrentDownloads = this[Keys.MAX_CONCURRENT]
            ?: UserPreferences.DEFAULT_CONCURRENT_DOWNLOADS,
        maxRetries = this[Keys.MAX_RETRIES] ?: UserPreferences.DEFAULT_MAX_RETRIES,
        autoUpdateYtDlp = this[Keys.AUTO_UPDATE_YTDLP] ?: true,
        autoUpdateFfmpeg = this[Keys.AUTO_UPDATE_FFMPEG] ?: false,
        defaultQuality = VideoQuality.fromNameOrDefault(this[Keys.DEFAULT_QUALITY]),
        defaultAudioFormat = AudioFormat.fromNameOrDefault(this[Keys.DEFAULT_AUDIO_FORMAT]),
        notificationsEnabled = this[Keys.NOTIFICATIONS_ENABLED] ?: true,
        cookiesFileUri = this[Keys.COOKIES_URI],
        proxyUrl = this[Keys.PROXY_URL],
        customHttpHeaders = this[Keys.CUSTOM_HEADERS],
        outputTemplate = this[Keys.OUTPUT_TEMPLATE] ?: UserPreferences.DEFAULT_OUTPUT_TEMPLATE,
        embedMetadata = this[Keys.EMBED_METADATA] ?: true,
        embedThumbnail = this[Keys.EMBED_THUMBNAIL] ?: false,
        downloadSubtitles = this[Keys.DOWNLOAD_SUBTITLES] ?: false,
        subtitleLanguages = this[Keys.SUBTITLE_LANGUAGES]
            ?: UserPreferences.DEFAULT_SUBTITLE_LANGUAGES,
        useDownloadArchive = this[Keys.USE_DOWNLOAD_ARCHIVE] ?: false,
        restrictFilenames = this[Keys.RESTRICT_FILENAMES] ?: true,
        autoResumeAfterBoot = this[Keys.AUTO_RESUME_AFTER_BOOT] ?: false,
        recentUrls = this[Keys.RECENT_URLS].orEmpty().splitRecents(),
    )

    private fun String.splitRecents(): List<String> =
        if (isBlank()) emptyList() else split(RECENT_SEPARATOR).filter { it.isNotBlank() }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DOWNLOAD_TREE_URI = stringPreferencesKey("download_tree_uri")
        val DOWNLOAD_DIRECTORY_LABEL = stringPreferencesKey("download_directory_label")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent_downloads")
        val MAX_RETRIES = intPreferencesKey("max_retries")
        val AUTO_UPDATE_YTDLP = booleanPreferencesKey("auto_update_ytdlp")
        val AUTO_UPDATE_FFMPEG = booleanPreferencesKey("auto_update_ffmpeg")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val DEFAULT_AUDIO_FORMAT = stringPreferencesKey("default_audio_format")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val COOKIES_URI = stringPreferencesKey("cookies_uri")
        val PROXY_URL = stringPreferencesKey("proxy_url")
        val CUSTOM_HEADERS = stringPreferencesKey("custom_headers")
        val OUTPUT_TEMPLATE = stringPreferencesKey("output_template")
        val EMBED_METADATA = booleanPreferencesKey("embed_metadata")
        val EMBED_THUMBNAIL = booleanPreferencesKey("embed_thumbnail")
        val DOWNLOAD_SUBTITLES = booleanPreferencesKey("download_subtitles")
        val SUBTITLE_LANGUAGES = stringPreferencesKey("subtitle_languages")
        val USE_DOWNLOAD_ARCHIVE = booleanPreferencesKey("use_download_archive")
        val RESTRICT_FILENAMES = booleanPreferencesKey("restrict_filenames")
        val AUTO_RESUME_AFTER_BOOT = booleanPreferencesKey("auto_resume_after_boot")
        val RECENT_URLS = stringPreferencesKey("recent_urls")
    }

    private companion object {
        const val RECENT_SEPARATOR = "\n"
    }
}

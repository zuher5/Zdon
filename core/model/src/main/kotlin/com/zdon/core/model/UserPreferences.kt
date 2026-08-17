package com.zdon.core.model

/**
 * User preferences persisted in DataStore. Defaults are chosen so a fresh
 * install can download successfully without visiting the settings screen.
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val downloadTreeUri: String? = null,
    val downloadDirectoryLabel: String? = null,
    val maxConcurrentDownloads: Int = DEFAULT_CONCURRENT_DOWNLOADS,
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val autoUpdateYtDlp: Boolean = true,
    val autoUpdateFfmpeg: Boolean = false,
    val defaultQuality: VideoQuality = VideoQuality.BEST,
    val defaultAudioFormat: AudioFormat = AudioFormat.MP3,
    val notificationsEnabled: Boolean = true,
    val cookiesFileUri: String? = null,
    val proxyUrl: String? = null,
    val customHttpHeaders: String? = null,
    val outputTemplate: String = DEFAULT_OUTPUT_TEMPLATE,
    val embedMetadata: Boolean = true,
    val embedThumbnail: Boolean = false,
    val downloadSubtitles: Boolean = false,
    val subtitleLanguages: String = DEFAULT_SUBTITLE_LANGUAGES,
    val useDownloadArchive: Boolean = false,
    val restrictFilenames: Boolean = true,
    val autoResumeAfterBoot: Boolean = false,
    val recentUrls: List<String> = emptyList(),
) {
    val hasDownloadLocation: Boolean
        get() = !downloadTreeUri.isNullOrBlank()

    companion object {
        const val DEFAULT_CONCURRENT_DOWNLOADS: Int = 2
        const val MIN_CONCURRENT_DOWNLOADS: Int = 1
        const val MAX_CONCURRENT_DOWNLOADS: Int = 6
        const val DEFAULT_MAX_RETRIES: Int = 3
        const val MIN_MAX_RETRIES: Int = 0
        const val MAX_MAX_RETRIES: Int = 10
        const val DEFAULT_SUBTITLE_LANGUAGES: String = "en"
        const val DEFAULT_OUTPUT_TEMPLATE: String = "%(title).200B.%(ext)s"
        const val MAX_RECENT_URLS: Int = 10
    }
}

package com.zdon.core.data.repository

import android.content.Context
import com.zdon.core.common.di.Dispatcher
import com.zdon.core.common.di.ZdonDispatcher
import com.zdon.core.common.url.UrlValidationResult
import com.zdon.core.common.url.UrlValidator
import com.zdon.core.data.R
import com.zdon.core.datastore.UserPreferencesDataSource
import com.zdon.core.downloader.storage.CookieFileProvider
import com.zdon.core.downloader.storage.DownloadStorageManager
import com.zdon.core.engine.EngineOptions
import com.zdon.core.engine.MediaInfoResult
import com.zdon.core.engine.YtDlpEngine
import com.zdon.core.engine.YtDlpInitializer
import com.zdon.core.model.BinaryUpdateResult
import com.zdon.core.model.DownloadErrorType
import com.zdon.core.model.EngineStatus
import com.zdon.core.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [MediaRepository]. Validates the URL before spending a process on it and
 * resolves the same proxy/cookie/header settings the downloader uses, so a URL
 * that analyses successfully will also download successfully.
 */
@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: YtDlpEngine,
    private val initializer: YtDlpInitializer,
    private val preferences: UserPreferencesDataSource,
    private val storageManager: DownloadStorageManager,
    private val cookieFileProvider: CookieFileProvider,
    @Dispatcher(ZdonDispatcher.IO) private val ioDispatcher: CoroutineDispatcher,
) : MediaRepository {

    override suspend fun analyze(url: String, includePlaylist: Boolean): AnalyzeResult =
        withContext(ioDispatcher) {
            val validation = UrlValidator.validate(url)
            if (validation is UrlValidationResult.Invalid) {
                return@withContext AnalyzeResult.Failure(
                    DownloadErrorType.UNSUPPORTED_URL,
                    context.getString(R.string.media_invalid_url),
                )
            }
            val normalizedUrl = (validation as UrlValidationResult.Valid).url

            val settings = preferences.currentPreferences()
            val options = buildAnalyzeOptions(settings)

            when (val result = engine.fetchMediaInfo(normalizedUrl, options, includePlaylist)) {
                is MediaInfoResult.Success -> AnalyzeResult.Success(result.mediaInfo)
                is MediaInfoResult.Failure -> AnalyzeResult.Failure(
                    result.errorType,
                    result.message,
                )
            }
        }

    override fun observeEngineStatus(): Flow<EngineStatus> = initializer.status

    override suspend fun initializeEngine() {
        initializer.ensureInitialized()
    }

    override suspend fun updateYtDlp(): BinaryUpdateResult = initializer.updateYtDlp()

    override suspend fun refreshFfmpeg(): BinaryUpdateResult = initializer.refreshFfmpeg()

    /**
     * Metadata extraction writes nothing, but yt-dlp still requires a working
     * directory, so the private staging area for id `0` is reused.
     */
    private suspend fun buildAnalyzeOptions(settings: UserPreferences): EngineOptions =
        EngineOptions(
            workingDirectoryPath = storageManager
                .stagingDirectory(ANALYZE_WORKSPACE_ID)
                .absolutePath,
            temporaryDirectoryPath = storageManager
                .temporaryDirectory(ANALYZE_WORKSPACE_ID)
                .absolutePath,
            outputTemplate = UserPreferences.DEFAULT_OUTPUT_TEMPLATE,
            retries = ANALYZE_RETRIES,
            proxyUrl = settings.proxyUrl,
            cookiesFilePath = cookieFileProvider.materialize(settings.cookiesFileUri),
            customHeaders = settings.customHttpHeaders
                ?.lineSequence()
                ?.mapNotNull(com.zdon.core.common.util.ArgumentSanitizer::sanitizeHeader)
                ?.toList()
                .orEmpty(),
            downloadArchivePath = null,
            useDownloadArchive = false,
            restrictFilenames = settings.restrictFilenames,
        )

    private companion object {
        const val ANALYZE_WORKSPACE_ID = 0L
        const val ANALYZE_RETRIES = 2
    }
}

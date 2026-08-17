package com.zdon.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zdon.core.common.url.UrlError
import com.zdon.core.common.url.UrlValidationResult
import com.zdon.core.common.url.UrlValidator
import com.zdon.core.data.repository.AnalyzeResult
import com.zdon.core.data.repository.DownloadRepository
import com.zdon.core.data.repository.MediaRepository
import com.zdon.core.data.repository.SettingsRepository
import com.zdon.core.model.AudioFormat
import com.zdon.core.model.DownloadRequest
import com.zdon.core.model.MediaInfo
import com.zdon.core.model.VideoQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Drives the home screen: URL entry, analysis and enqueueing.
 *
 * The analysis job is tracked so a new request cancels the previous one; this
 * prevents a slow yt-dlp process from overwriting fresher results and stops the
 * process when the user edits the URL.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val downloadRepository: DownloadRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>(
        replay = 0,
        extraBufferCapacity = EVENT_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private var analyzeJob: Job? = null

    init {
        observePreferences()
        observeActiveCount()
        prepareEngine()
    }

    /** Called on every keystroke; validation is cheap and purely local. */
    fun onUrlChanged(value: String) {
        val validation = UrlValidator.validate(value)
        _uiState.update { state ->
            state.copy(
                urlInput = value,
                isUrlValid = validation is UrlValidationResult.Valid,
                urlError = (validation as? UrlValidationResult.Invalid)
                    ?.error
                    ?.toInputError(),
                analyzeError = null,
            )
        }
    }

    /** Replaces the field with clipboard content and immediately analyses it. */
    fun onUrlPasted(value: String) {
        onUrlChanged(value)
        if (_uiState.value.isUrlValid) analyze()
    }

    private var lastSharedUrl: String? = null

    /**
     * Consumes a URL shared into the app from another application. The URL is
     * pasted and analysed exactly once per distinct value, so recomposing the
     * home screen (e.g. switching tabs) never re-runs the analysis while a new
     * share still gets through.
     */
    fun onSharedUrlProvided(url: String) {
        if (url == lastSharedUrl) return
        lastSharedUrl = url
        onUrlPasted(url)
    }

    fun onRecentUrlSelected(url: String) {
        onUrlChanged(url)
        analyze()
    }

    fun clearUrl() {
        analyzeJob?.cancel()
        _uiState.update {
            it.copy(
                urlInput = "",
                isUrlValid = false,
                urlError = null,
                mediaInfo = null,
                analyzeError = null,
                selectedFormatId = null,
                isAnalyzing = false,
            )
        }
    }

    /** Fetches metadata and formats for the current URL. */
    fun analyze() {
        val state = _uiState.value
        val validation = UrlValidator.validate(state.urlInput)
        if (validation !is UrlValidationResult.Valid) {
            _uiState.update {
                it.copy(urlError = (validation as UrlValidationResult.Invalid).error.toInputError())
            }
            return
        }

        analyzeJob?.cancel()
        analyzeJob = viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, analyzeError = null, mediaInfo = null) }

            val includePlaylist = UrlValidator.looksLikePlaylist(validation.url)
            when (val result = mediaRepository.analyze(validation.url, includePlaylist)) {
                is AnalyzeResult.Success -> applyAnalysis(result.mediaInfo)
                is AnalyzeResult.Failure -> _uiState.update {
                    it.copy(isAnalyzing = false, analyzeError = result.message)
                }
            }
        }
    }

    fun onQualitySelected(quality: VideoQuality) {
        _uiState.update { state ->
            state.copy(
                selectedQuality = quality,
                selectedFormatId = null,
                // Choosing an audio-only quality forces extraction; for video
                // qualities the user's extract-audio toggle is kept as-is.
                extractAudio = quality.isAudioOnly,
            )
        }
    }

    fun onFormatSelected(formatId: String) {
        _uiState.update { state ->
            val format = state.mediaInfo?.formats?.firstOrNull { it.formatId == formatId }
            state.copy(
                selectedFormatId = formatId,
                selectedQuality = VideoQuality.CUSTOM,
                extractAudio = format?.isAudioOnly ?: state.extractAudio,
            )
        }
    }

    fun onExtractAudioChanged(enabled: Boolean) {
        _uiState.update { it.copy(extractAudio = enabled, selectedFormatId = null) }
    }

    fun onAudioFormatSelected(format: AudioFormat) {
        _uiState.update { it.copy(audioFormat = format) }
    }

    fun onDownloadSubtitlesChanged(enabled: Boolean) {
        _uiState.update { it.copy(downloadSubtitles = enabled) }
    }

    fun onEmbedThumbnailChanged(enabled: Boolean) {
        _uiState.update { it.copy(embedThumbnail = enabled) }
    }

    fun onEmbedMetadataChanged(enabled: Boolean) {
        _uiState.update { it.copy(embedMetadata = enabled) }
    }

    fun onDownloadPlaylistChanged(enabled: Boolean) {
        _uiState.update { it.copy(downloadPlaylist = enabled) }
    }

    fun onCustomFileNameChanged(value: String) {
        _uiState.update { it.copy(customFileName = value) }
    }

    /**
     * Enqueues the current selection. For a playlist with [HomeUiState.downloadPlaylist]
     * enabled, one request is created per entry so each row can be tracked,
     * retried and cancelled independently.
     */
    fun download() {
        val state = _uiState.value
        if (!state.canDownload) return

        viewModelScope.launch {
            if (!settingsRepository.hasWritableDownloadLocation()) {
                _uiState.update { it.copy(hasDownloadFolder = false) }
                _events.tryEmit(HomeEvent.RequestDownloadFolder)
                return@launch
            }

            _uiState.update { it.copy(isEnqueueing = true) }
            try {
                val requests = buildRequests(state)
                downloadRepository.enqueueAll(requests)
                _events.tryEmit(HomeEvent.DownloadQueued(requests.size))
                _uiState.update {
                    it.copy(isEnqueueing = false, urlInput = "", isUrlValid = false, mediaInfo = null)
                }
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Failed to enqueue download")
                _uiState.update { it.copy(isEnqueueing = false) }
                _events.tryEmit(
                    HomeEvent.ShowMessage(
                        throwable.message ?: context.getString(R.string.home_enqueue_failed),
                    ),
                )
            }
        }
    }

    private fun buildRequests(state: HomeUiState): List<DownloadRequest> {
        val info = state.mediaInfo
        val settings = state.toRequestTemplate()

        return when {
            info != null && info.isPlaylist && state.downloadPlaylist && info.entries.isNotEmpty() ->
                info.entries.map { entry ->
                    settings.copy(
                        url = entry.webpageUrl ?: entry.originalUrl,
                        title = entry.title,
                        thumbnailUrl = entry.thumbnailUrl,
                        uploader = entry.uploader,
                        durationSeconds = entry.durationSeconds,
                        isPlaylist = false,
                        // A per-entry format id from the parent listing would not
                        // apply to the child media.
                        customFormatId = null,
                    )
                }

            else -> listOf(settings)
        }
    }

    private fun HomeUiState.toRequestTemplate(): DownloadRequest {
        val info = mediaInfo
        val resolvedUrl = info?.webpageUrl ?: info?.originalUrl ?: urlInput.trim()
        return DownloadRequest(
            url = resolvedUrl,
            title = info?.title ?: resolvedUrl,
            quality = selectedQuality,
            audioFormat = audioFormat,
            customFormatId = selectedFormatId,
            extractAudio = extractAudio || selectedQuality.isAudioOnly,
            downloadSubtitles = downloadSubtitles,
            subtitleLanguages = info?.subtitleLanguages?.take(MAX_SUBTITLE_LANGUAGES).orEmpty(),
            embedSubtitles = downloadSubtitles,
            downloadThumbnail = embedThumbnail,
            embedThumbnail = embedThumbnail,
            embedMetadata = embedMetadata,
            isPlaylist = downloadPlaylist && info?.isPlaylist == true,
            outputTemplate = customFileName.takeIf { it.isNotBlank() }
                ?.let { "$it.%(ext)s" },
            thumbnailUrl = info?.thumbnailUrl,
            uploader = info?.uploader,
            durationSeconds = info?.durationSeconds ?: 0L,
        )
    }

    private fun applyAnalysis(mediaInfo: MediaInfo) {
        _uiState.update { state ->
            state.copy(
                isAnalyzing = false,
                mediaInfo = mediaInfo,
                analyzeError = null,
                downloadPlaylist = mediaInfo.isPlaylist,
                selectedFormatId = null,
            )
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            settingsRepository.userPreferences.collect { preferences ->
                _uiState.update { state ->
                    state.copy(
                        recentUrls = preferences.recentUrls,
                        selectedQuality = if (state.mediaInfo == null && !state.isAnalyzing) {
                            preferences.defaultQuality
                        } else {
                            state.selectedQuality
                        },
                        audioFormat = preferences.defaultAudioFormat,
                        embedMetadata = preferences.embedMetadata,
                        embedThumbnail = preferences.embedThumbnail,
                        downloadSubtitles = preferences.downloadSubtitles,
                        hasDownloadFolder = preferences.hasDownloadLocation,
                    )
                }
            }
        }
    }

    private fun observeActiveCount() {
        viewModelScope.launch {
            downloadRepository.observeActiveCount().collect { count ->
                _uiState.update { it.copy(activeDownloadCount = count) }
            }
        }
    }

    /**
     * Unpacking the Python runtime takes a few seconds on first launch, so it is
     * started as soon as the screen opens rather than when the user taps analyse.
     */
    private fun prepareEngine() {
        viewModelScope.launch { mediaRepository.initializeEngine() }
    }

    private fun UrlError.toInputError(): UrlInputError? = when (this) {
        UrlError.EMPTY -> null
        UrlError.MALFORMED -> UrlInputError.MALFORMED
        UrlError.UNSUPPORTED_SCHEME -> UrlInputError.UNSUPPORTED_SCHEME
        UrlError.ILLEGAL_CHARACTERS -> UrlInputError.ILLEGAL_CHARACTERS
    }

    private companion object {
        const val EVENT_BUFFER = 4
        const val MAX_SUBTITLE_LANGUAGES = 3
    }
}

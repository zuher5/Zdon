package com.zdon.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zdon.core.data.repository.MediaRepository
import com.zdon.core.data.repository.SettingsRepository
import com.zdon.core.model.AudioFormat
import com.zdon.core.model.BinaryUpdateResult
import com.zdon.core.model.ThemeMode
import com.zdon.core.model.VideoQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Settings state holder.
 *
 * Every setter writes straight through to the repository; the screen renders the
 * persisted value that comes back through the preferences flow, so there is no
 * second copy of the state to get out of sync.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val mediaRepository: MediaRepository,
) : ViewModel() {

    private val isUpdatingBinary = MutableStateFlow(false)
    private val notificationsBlocked = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.userPreferences,
        mediaRepository.observeEngineStatus(),
        isUpdatingBinary,
        notificationsBlocked,
    ) { preferences, engineStatus, updating, blocked ->
        SettingsUiState(
            preferences = preferences,
            engineStatus = engineStatus,
            isUpdatingBinary = updating,
            notificationsBlockedBySystem = blocked,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(),
    )

    private val _events = MutableSharedFlow<SettingsEvent>(
        replay = 0,
        extraBufferCapacity = EVENT_BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch { mediaRepository.initializeEngine() }
    }

    fun onThemeModeSelected(themeMode: ThemeMode) = write {
        settingsRepository.setThemeMode(themeMode)
    }

    fun onDynamicColorChanged(enabled: Boolean) = write {
        settingsRepository.setUseDynamicColor(enabled)
    }

    fun onDownloadFolderSelected(treeUri: String) = write {
        val accepted = settingsRepository.setDownloadLocation(treeUri)
        if (!accepted) _events.tryEmit(SettingsEvent.ShowMessage(R.string.settings_folder_denied))
    }

    fun onConcurrentDownloadsChanged(value: Int) = write {
        settingsRepository.setMaxConcurrentDownloads(value)
    }

    fun onMaxRetriesChanged(value: Int) = write {
        settingsRepository.setMaxRetries(value)
    }

    fun onDefaultQualitySelected(quality: VideoQuality) = write {
        settingsRepository.setDefaultQuality(quality)
    }

    fun onDefaultAudioFormatSelected(format: AudioFormat) = write {
        settingsRepository.setDefaultAudioFormat(format)
    }

    fun onAutoUpdateYtDlpChanged(enabled: Boolean) = write {
        settingsRepository.setAutoUpdateYtDlp(enabled)
    }

    fun onAutoUpdateFfmpegChanged(enabled: Boolean) = write {
        settingsRepository.setAutoUpdateFfmpeg(enabled)
    }

    fun onNotificationsChanged(enabled: Boolean) = write {
        settingsRepository.setNotificationsEnabled(enabled)
    }

    fun onNotificationsBlocked(blocked: Boolean) {
        notificationsBlocked.value = blocked
    }

    fun onCookiesFileSelected(uri: String?) = write {
        settingsRepository.setCookiesFileUri(uri)
    }

    fun onProxyChanged(proxy: String) = write {
        settingsRepository.setProxyUrl(proxy.trim().takeIf { it.isNotEmpty() })
    }

    fun onCustomHeadersChanged(headers: String) = write {
        settingsRepository.setCustomHttpHeaders(headers.takeIf { it.isNotBlank() })
    }

    fun onOutputTemplateChanged(template: String) = write {
        settingsRepository.setOutputTemplate(template)
    }

    fun onEmbedMetadataChanged(enabled: Boolean) = write {
        settingsRepository.setEmbedMetadata(enabled)
    }

    fun onEmbedThumbnailChanged(enabled: Boolean) = write {
        settingsRepository.setEmbedThumbnail(enabled)
    }

    fun onDownloadSubtitlesChanged(enabled: Boolean) = write {
        settingsRepository.setDownloadSubtitles(enabled)
    }

    fun onSubtitleLanguagesChanged(languages: String) = write {
        settingsRepository.setSubtitleLanguages(languages)
    }

    fun onDownloadArchiveChanged(enabled: Boolean) = write {
        settingsRepository.setUseDownloadArchive(enabled)
    }

    fun onRestrictFilenamesChanged(enabled: Boolean) = write {
        settingsRepository.setRestrictFilenames(enabled)
    }

    fun onClearRecentUrls() = write { settingsRepository.clearRecentUrls() }

    /** Fetches the newest yt-dlp build on demand. */
    fun updateYtDlpNow() {
        if (isUpdatingBinary.value) return
        viewModelScope.launch {
            isUpdatingBinary.value = true
            try {
                val messageRes = when (mediaRepository.updateYtDlp()) {
                    BinaryUpdateResult.UPDATED -> R.string.settings_update_done
                    BinaryUpdateResult.ALREADY_UP_TO_DATE -> R.string.settings_update_current
                    BinaryUpdateResult.FAILED -> R.string.settings_update_failed
                }
                _events.tryEmit(SettingsEvent.ShowMessage(messageRes))
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Timber.e(throwable, "yt-dlp update failed")
                _events.tryEmit(SettingsEvent.ShowMessage(R.string.settings_update_failed))
            } finally {
                isUpdatingBinary.value = false
            }
        }
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Failed to persist setting")
            }
        }
    }

    private companion object {
        const val EVENT_BUFFER = 4
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

package com.zdon.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zdon.core.data.repository.DownloadRepository
import com.zdon.core.data.repository.SettingsRepository
import com.zdon.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity-scoped state: the theme to apply and the badge count for the downloads
 * tab. Kept separate from the feature ViewModels so the theme is resolved once,
 * before the first frame, avoiding a white flash on a dark-theme device.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    downloadRepository: DownloadRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        settingsRepository.userPreferences,
        downloadRepository.observeActiveCount(),
    ) { preferences, activeCount ->
        MainUiState.Ready(
            themeMode = preferences.themeMode,
            useDynamicColor = preferences.useDynamicColor,
            activeDownloadCount = activeCount,
            hasDownloadFolder = preferences.hasDownloadLocation,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MainUiState.Loading,
    )

    /** Persists the folder the user picked through the system document picker. */
    fun onDownloadFolderSelected(treeUri: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setDownloadLocation(treeUri)
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Unable to persist the download folder")
            }
        }
    }
}

/** Startup state of the activity. */
sealed interface MainUiState {

    data object Loading : MainUiState

    data class Ready(
        val themeMode: ThemeMode,
        val useDynamicColor: Boolean,
        val activeDownloadCount: Int,
        val hasDownloadFolder: Boolean,
    ) : MainUiState

    val shouldKeepSplashScreen: Boolean
        get() = this is Loading

    val themeModeOrDefault: ThemeMode
        get() = (this as? Ready)?.themeMode ?: ThemeMode.SYSTEM

    val dynamicColorOrDefault: Boolean
        get() = (this as? Ready)?.useDynamicColor ?: true

    val activeCountOrZero: Int
        get() = (this as? Ready)?.activeDownloadCount ?: 0
}

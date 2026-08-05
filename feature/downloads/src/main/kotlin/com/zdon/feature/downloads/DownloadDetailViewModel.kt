package com.zdon.feature.downloads

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zdon.core.data.repository.DownloadRepository
import com.zdon.core.model.DownloadItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * State holder for the single-download detail screen.
 *
 * The id is read from [SavedStateHandle], so the screen survives process death
 * and reconstructs itself from the navigation argument alone.
 */
@HiltViewModel
class DownloadDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val downloadId: Long = savedStateHandle.get<String>(ARG_DOWNLOAD_ID)
        ?.toLongOrNull()
        ?: savedStateHandle.get<Long>(ARG_DOWNLOAD_ID)
        ?: INVALID_ID

    val uiState: StateFlow<DownloadDetailUiState> = downloadRepository
        .observeDownload(downloadId)
        .map { item -> DownloadDetailUiState(isLoading = false, download = item) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DownloadDetailUiState(),
        )

    fun pause() = launchAction { downloadRepository.pause(downloadId) }

    fun resume() = launchAction { downloadRepository.resume(downloadId) }

    fun cancel() = launchAction { downloadRepository.cancel(downloadId) }

    fun retry() = launchAction { downloadRepository.retry(downloadId) }

    private fun launchAction(block: suspend () -> Unit) {
        if (downloadId == INVALID_ID) return
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Detail action failed for download %d", downloadId)
            }
        }
    }

    companion object {
        const val ARG_DOWNLOAD_ID = "downloadId"
        private const val INVALID_ID = -1L
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** State of the download detail screen. */
data class DownloadDetailUiState(
    val isLoading: Boolean = true,
    val download: DownloadItem? = null,
)

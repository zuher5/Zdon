package com.zdon.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zdon.core.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Exposes the download queue and forwards user actions to the repository.
 *
 * The state flow is built with `stateIn(WhileSubscribed)` so the Room query is
 * stopped when the screen is not visible and restarted on return, which avoids
 * pointless work while the app is backgrounded.
 */
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(DownloadFilter.ALL)

    val uiState: StateFlow<DownloadsUiState> = combine(
        downloadRepository.observeDownloads(),
        filter,
    ) { downloads, activeFilter ->
        DownloadsUiState(
            isLoading = false,
            downloads = downloads,
            filter = activeFilter,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DownloadsUiState(),
    )

    fun onFilterSelected(value: DownloadFilter) {
        filter.value = value
    }

    fun pause(id: Long) = launchAction("pause") { downloadRepository.pause(id) }

    fun resume(id: Long) = launchAction("resume") { downloadRepository.resume(id) }

    fun cancel(id: Long) = launchAction("cancel") { downloadRepository.cancel(id) }

    fun retry(id: Long) = launchAction("retry") { downloadRepository.retry(id) }

    fun remove(id: Long) = launchAction("remove") { downloadRepository.remove(id) }

    fun cancelAll() = launchAction("cancelAll") { downloadRepository.cancelAll() }

    fun clearFinished() = launchAction("clearFinished") { downloadRepository.clearFinished() }

    private fun launchAction(name: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Download action %s failed", name)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

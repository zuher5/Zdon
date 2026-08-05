package com.zdon.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zdon.core.data.repository.DownloadRepository
import com.zdon.core.data.repository.HistoryRepository
import com.zdon.core.model.DownloadRequest
import com.zdon.core.model.VideoQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Exposes the download history with debounced search.
 *
 * Debouncing keeps a fresh SQL query from running on every keystroke;
 * `flatMapLatest` cancels the previous query so results always match the latest
 * text the user typed.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<HistoryUiState> = query
        .debounce { text -> if (text.isEmpty()) 0L else SEARCH_DEBOUNCE_MILLIS }
        .flatMapLatest { text ->
            historyRepository.observeHistory(text).map { entries -> text to entries }
        }
        .map { (text, entries) ->
            HistoryUiState(isLoading = false, entries = entries, query = text)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HistoryUiState(),
        )

    fun onQueryChanged(value: String) {
        query.value = value
    }

    fun clearQuery() {
        query.value = ""
    }

    fun delete(id: Long) = launchAction { historyRepository.delete(id) }

    fun clearAll() = launchAction { historyRepository.clear() }

    /** Re-queues a previously downloaded link using the app defaults. */
    fun redownload(url: String, title: String, thumbnailUrl: String?, uploader: String?) =
        launchAction {
            downloadRepository.enqueue(
                DownloadRequest(
                    url = url,
                    title = title,
                    quality = VideoQuality.BEST,
                    audioFormat = com.zdon.core.model.AudioFormat.MP3,
                    thumbnailUrl = thumbnailUrl,
                    uploader = uploader,
                ),
            )
        }

    private fun launchAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                Timber.e(throwable, "History action failed")
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 250L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

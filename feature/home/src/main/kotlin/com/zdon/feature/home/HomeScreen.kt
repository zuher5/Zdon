package com.zdon.feature.home

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zdon.core.designsystem.component.ZdonEmptyState
import com.zdon.core.designsystem.util.isExpandedWidth
import com.zdon.core.model.AudioFormat
import com.zdon.core.model.VideoQuality
import com.zdon.feature.home.component.DownloadOptionsCard
import com.zdon.feature.home.component.FormatRow
import com.zdon.feature.home.component.MediaInfoCard
import com.zdon.feature.home.component.QualitySelector
import com.zdon.feature.home.component.RecentUrlsCard
import com.zdon.feature.home.component.UrlInputCard

/**
 * Home route. Owns clipboard access and delegates every state change to
 * [HomeViewModel], keeping the composable free of business logic.
 *
 * [initialSharedUrl] is a URL shared into the app from another application; it
 * is pasted and analysed once, the first time this screen is composed.
 */
@Composable
fun HomeRoute(
    onChooseFolder: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    initialSharedUrl: String? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardEmptyMessage = stringResource(R.string.home_clipboard_empty)

    androidx.compose.runtime.LaunchedEffect(viewModel, initialSharedUrl) {
        if (!initialSharedUrl.isNullOrBlank()) {
            viewModel.onSharedUrlProvided(initialSharedUrl)
        }
    }

    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.DownloadQueued -> onShowMessage(
                    context.resources.getQuantityString(
                        R.plurals.home_queued,
                        event.count,
                        event.count,
                    ),
                )
                is HomeEvent.ShowMessage -> onShowMessage(event.message)
                HomeEvent.RequestDownloadFolder -> onChooseFolder()
            }
        }
    }

    HomeScreen(
        state = state,
        modifier = modifier,
        onUrlChange = viewModel::onUrlChanged,
        onPasteClick = {
            val pasted = context.readClipboardText()
            if (pasted.isNullOrBlank()) onShowMessage(clipboardEmptyMessage) else viewModel.onUrlPasted(pasted)
        },
        onClearClick = viewModel::clearUrl,
        onAnalyzeClick = viewModel::analyze,
        onDownloadClick = viewModel::download,
        onRecentUrlClick = viewModel::onRecentUrlSelected,
        onQualitySelected = viewModel::onQualitySelected,
        onFormatSelected = viewModel::onFormatSelected,
        onExtractAudioChange = viewModel::onExtractAudioChanged,
        onAudioFormatSelected = viewModel::onAudioFormatSelected,
        onDownloadSubtitlesChange = viewModel::onDownloadSubtitlesChanged,
        onEmbedThumbnailChange = viewModel::onEmbedThumbnailChanged,
        onEmbedMetadataChange = viewModel::onEmbedMetadataChanged,
        onDownloadPlaylistChange = viewModel::onDownloadPlaylistChanged,
        onCustomFileNameChange = viewModel::onCustomFileNameChanged,
        onChooseFolder = onChooseFolder,
    )
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onUrlChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onClearClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onRecentUrlClick: (String) -> Unit,
    onQualitySelected: (VideoQuality) -> Unit,
    onFormatSelected: (String) -> Unit,
    onExtractAudioChange: (Boolean) -> Unit,
    onAudioFormatSelected: (AudioFormat) -> Unit,
    onDownloadSubtitlesChange: (Boolean) -> Unit,
    onEmbedThumbnailChange: (Boolean) -> Unit,
    onEmbedMetadataChange: (Boolean) -> Unit,
    onDownloadPlaylistChange: (Boolean) -> Unit,
    onCustomFileNameChange: (String) -> Unit,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = if (isExpandedWidth()) 32.dp else 16.dp

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 16.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "url-input") {
            UrlInputCard(
                url = state.urlInput,
                error = state.urlError,
                isAnalyzing = state.isAnalyzing,
                canAnalyze = state.canDownload,
                onUrlChange = onUrlChange,
                onPasteClick = onPasteClick,
                onClearClick = onClearClick,
                onAnalyzeClick = onAnalyzeClick,
                onDownloadClick = onDownloadClick,
            )
        }

        if (!state.hasDownloadFolder) {
            item(key = "folder-warning") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ZdonEmptyState(
                            icon = Icons.Rounded.FolderOpen,
                            title = stringResource(R.string.home_no_folder_title),
                            description = stringResource(R.string.home_no_folder_description),
                            actionLabel = stringResource(R.string.home_choose_folder),
                            onActionClick = onChooseFolder,
                        )
                    }
                }
            }
        }

        if (state.isAnalyzing) {
            item(key = "analyzing") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.home_analyzing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        state.analyzeError?.let { message ->
            item(key = "analyze-error") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.home_analysis_failed_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        state.mediaInfo?.let { mediaInfo ->
            item(key = "media-info") {
                MediaInfoCard(
                    mediaInfo = mediaInfo,
                    estimatedSizeBytes = state.estimatedSizeBytes,
                )
            }

            item(key = "quality") {
                QualitySelector(
                    selectedQuality = state.selectedQuality,
                    availableResolutions = mediaInfo.availableResolutions,
                    hasAudioFormats = state.audioFormats.isNotEmpty(),
                    onQualitySelected = onQualitySelected,
                )
            }

            item(key = "options") {
                DownloadOptionsCard(
                    state = state,
                    onExtractAudioChange = onExtractAudioChange,
                    onAudioFormatSelected = onAudioFormatSelected,
                    onDownloadSubtitlesChange = onDownloadSubtitlesChange,
                    onEmbedThumbnailChange = onEmbedThumbnailChange,
                    onEmbedMetadataChange = onEmbedMetadataChange,
                    onDownloadPlaylistChange = onDownloadPlaylistChange,
                    onCustomFileNameChange = onCustomFileNameChange,
                )
            }

            if (state.videoFormats.isEmpty() && state.audioFormats.isEmpty()) {
                item(key = "no-formats") {
                    Text(
                        text = stringResource(R.string.home_no_formats),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.videoFormats.isNotEmpty()) {
                item(key = "video-header") {
                    Text(
                        text = stringResource(R.string.home_video_formats_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                items(
                    items = state.videoFormats,
                    key = { "video-${it.formatId}" },
                ) { format ->
                    FormatRow(
                        format = format,
                        isSelected = state.selectedFormatId == format.formatId,
                        onClick = { onFormatSelected(format.formatId) },
                    )
                }
            }

            if (state.audioFormats.isNotEmpty()) {
                item(key = "audio-header") {
                    Text(
                        text = stringResource(R.string.home_audio_formats_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                items(
                    items = state.audioFormats,
                    key = { "audio-${it.formatId}" },
                ) { format ->
                    FormatRow(
                        format = format,
                        isSelected = state.selectedFormatId == format.formatId,
                        onClick = { onFormatSelected(format.formatId) },
                    )
                }
            }
        }

        if (!state.hasAnalysisResult) {
            item(key = "recent") {
                RecentUrlsCard(urls = state.recentUrls, onUrlClick = onRecentUrlClick)
            }
        }
    }
}

/**
 * Reads plain text from the clipboard.
 *
 * On Android 12+ this shows a system toast telling the user the app accessed the
 * clipboard, which is exactly the transparency we want for a paste button.
 */
private fun Context.readClipboardText(): String? {
    val clipboard = getSystemService<ClipboardManager>() ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(this).toString().trim().takeIf { it.isNotEmpty() }
}

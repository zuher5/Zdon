package com.zdon.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zdon.core.common.util.Formatters
import com.zdon.core.designsystem.component.ZdonEmptyState
import com.zdon.core.designsystem.component.ZdonLoadingIndicator
import com.zdon.core.designsystem.component.ZdonProgressBar
import com.zdon.core.designsystem.component.ZdonThumbnail
import com.zdon.core.model.DownloadItem
import com.zdon.core.model.DownloadStatus
import java.text.DateFormat
import java.util.Date

/** Detail route showing full progress telemetry and controls for one download. */
@Composable
fun DownloadDetailRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DownloadDetailScreen(
        state = state,
        modifier = modifier,
        onBackClick = onBackClick,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onCancel = viewModel::cancel,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadDetailScreen(
    state: DownloadDetailUiState,
    onBackClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.downloads_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.downloads_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val item = state.download

        when {
            state.isLoading -> ZdonLoadingIndicator(modifier = Modifier.padding(innerPadding))

            item == null -> ZdonEmptyState(
                icon = Icons.Rounded.ErrorOutline,
                title = stringResource(R.string.downloads_detail_missing),
                description = stringResource(R.string.downloads_empty_description),
                modifier = Modifier.padding(innerPadding),
            )

            else -> DownloadDetailContent(
                item = item,
                modifier = Modifier.padding(innerPadding),
                onPause = onPause,
                onResume = onResume,
                onCancel = onCancel,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun DownloadDetailContent(
    item: DownloadItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ZdonThumbnail(
            imageUrl = item.thumbnailUrl,
            contentDescription = item.title,
            modifier = Modifier.fillMaxWidth(),
            durationLabel = Formatters.formatDurationOrNull(item.durationSeconds),
        )

        Text(text = item.title, style = MaterialTheme.typography.titleMedium)

        ZdonProgressBar(
            progress = item.progressFraction,
            indeterminate = item.isIndeterminate,
            leadingLabel = Formatters.formatPercent(item.progressPercent),
            trailingLabel = Formatters.formatSpeedOrNull(item.speedBytesPerSecond),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            DetailField(
                label = stringResource(R.string.downloads_detail_status),
                value = stringResource(DownloadStrings.statusLabel(item.status)),
            )
            DetailField(
                label = stringResource(R.string.downloads_detail_downloaded),
                value = Formatters.formatBytes(item.downloadedBytes),
            )
            DetailField(
                label = stringResource(R.string.downloads_detail_total),
                value = Formatters.formatBytesOrNull(item.totalBytes)
                    ?: stringResource(R.string.downloads_status_queued),
            )
            if (item.remainingBytes > 0L) {
                DetailField(
                    label = stringResource(R.string.downloads_remaining, ""),
                    value = Formatters.formatBytes(item.remainingBytes),
                )
            }
            if (item.etaSeconds > 0L) {
                DetailField(
                    label = stringResource(R.string.downloads_detail_speed),
                    value = Formatters.formatDuration(item.etaSeconds),
                )
            }
            item.currentFragment?.let { fragment ->
                DetailField(
                    label = stringResource(R.string.downloads_fragment, ""),
                    value = fragment,
                )
            }
            DetailField(
                label = stringResource(R.string.downloads_detail_quality),
                value = item.quality.label,
            )
            item.formatId?.let { formatId ->
                DetailField(
                    label = stringResource(R.string.downloads_detail_format),
                    value = formatId,
                )
            }
            DetailField(
                label = stringResource(R.string.downloads_detail_url),
                value = item.url,
            )
            item.outputFileName?.let { fileName ->
                DetailField(
                    label = stringResource(R.string.downloads_detail_output),
                    value = fileName,
                )
            }
            DetailField(
                label = stringResource(R.string.downloads_detail_created),
                value = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(Date(item.createdAtMillis)),
            )
            item.errorType?.let { errorType ->
                DetailField(
                    label = stringResource(R.string.downloads_detail_error),
                    value = stringResource(DownloadStrings.errorLabel(errorType)),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (item.status == DownloadStatus.RUNNING) {
                OutlinedButton(onClick = onPause) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = stringResource(com.zdon.core.downloader.R.string.action_pause),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            if (item.status.canResume) {
                Button(onClick = onResume) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = stringResource(com.zdon.core.downloader.R.string.action_resume),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            if (item.status.canRetry) {
                Button(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = stringResource(com.zdon.core.downloader.R.string.action_retry),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            if (item.status.canCancel) {
                OutlinedButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Rounded.Cancel,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = stringResource(com.zdon.core.downloader.R.string.action_cancel),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DetailRow(label = label, value = value)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(WEIGHT_LABEL),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(WEIGHT_VALUE),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val WEIGHT_LABEL = 0.4f
private const val WEIGHT_VALUE = 0.6f

package com.zdon.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zdon.core.designsystem.component.ZdonEmptyState
import com.zdon.core.designsystem.component.ZdonLoadingIndicator
import com.zdon.core.designsystem.util.isExpandedWidth
import com.zdon.feature.downloads.component.DownloadRow

/** Downloads route: the live queue with per-item controls. */
@Composable
fun DownloadsRoute(
    onDownloadClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DownloadsScreen(
        state = state,
        modifier = modifier,
        onFilterSelected = viewModel::onFilterSelected,
        onDownloadClick = onDownloadClick,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onCancel = viewModel::cancel,
        onRetry = viewModel::retry,
        onRemove = viewModel::remove,
        onCancelAll = viewModel::cancelAll,
        onClearFinished = viewModel::clearFinished,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun DownloadsScreen(
    state: DownloadsUiState,
    onFilterSelected: (DownloadFilter) -> Unit,
    onDownloadClick: (Long) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onCancelAll: () -> Unit,
    onClearFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = if (isExpandedWidth()) 32.dp else 16.dp

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.downloads_title)) },
                actions = {
                    if (state.activeCount > 0) {
                        IconButton(onClick = onCancelAll) {
                            Icon(
                                imageVector = Icons.Rounded.StopCircle,
                                contentDescription = stringResource(
                                    R.string.downloads_cancel_all,
                                ),
                            )
                        }
                    }
                    if (state.hasFinished) {
                        IconButton(onClick = onClearFinished) {
                            Icon(
                                imageVector = Icons.Rounded.CleaningServices,
                                contentDescription = stringResource(
                                    R.string.downloads_clear_finished,
                                ),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when {
                state.isLoading -> ZdonLoadingIndicator()

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = 8.dp,
                        bottom = 32.dp,
                    ),
                ) {
                    item(key = "filters") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DownloadFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = state.filter == filter,
                                    onClick = { onFilterSelected(filter) },
                                    label = {
                                        Text(
                                            text = stringResource(
                                                DownloadStrings.filterLabel(filter),
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    if (state.isEmpty) {
                        item(key = "empty") {
                            val isFiltered = state.filter != DownloadFilter.ALL
                            ZdonEmptyState(
                                icon = Icons.Rounded.Download,
                                title = stringResource(
                                    if (isFiltered) {
                                        R.string.downloads_empty_filtered_title
                                    } else {
                                        R.string.downloads_empty_title
                                    },
                                ),
                                description = stringResource(
                                    if (isFiltered) {
                                        R.string.downloads_empty_filtered_description
                                    } else {
                                        R.string.downloads_empty_description
                                    },
                                ),
                            )
                        }
                    }

                    items(
                        items = state.visibleDownloads,
                        key = { it.id },
                    ) { item ->
                        DownloadRow(
                            item = item,
                            onClick = { onDownloadClick(item.id) },
                            onPause = { onPause(item.id) },
                            onResume = { onResume(item.id) },
                            onCancel = { onCancel(item.id) },
                            onRetry = { onRetry(item.id) },
                            onRemove = { onRemove(item.id) },
                        )
                    }
                }
            }
        }
    }
}

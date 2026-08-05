package com.zdon.feature.history

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zdon.core.designsystem.component.ZdonEmptyState
import com.zdon.core.designsystem.component.ZdonLoadingIndicator
import com.zdon.core.designsystem.util.isExpandedWidth
import com.zdon.core.model.HistoryEntry
import com.zdon.feature.history.component.HistoryRow

/** History route: searchable list of completed downloads. */
@Composable
fun HistoryRoute(
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val noPlayerMessage = stringResource(R.string.history_no_player)

    HistoryScreen(
        state = state,
        modifier = modifier,
        onQueryChange = viewModel::onQueryChanged,
        onClearQuery = viewModel::clearQuery,
        onClearAll = viewModel::clearAll,
        onDelete = viewModel::delete,
        onRedownload = { entry ->
            viewModel.redownload(
                url = entry.url,
                title = entry.title,
                thumbnailUrl = entry.thumbnailUrl,
                uploader = entry.uploader,
            )
        },
        onOpen = { entry ->
            val path = entry.filePath ?: return@HistoryScreen
            val opened = openMedia(context, path)
            if (!opened) onShowMessage(noPlayerMessage)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(
    state: HistoryUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onClearAll: () -> Unit,
    onDelete: (Long) -> Unit,
    onRedownload: (HistoryEntry) -> Unit,
    onOpen: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val horizontalPadding = if (isExpandedWidth()) 32.dp else 16.dp

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.history_title)) },
                actions = {
                    if (state.entries.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = stringResource(R.string.history_clear_all),
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "search") {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(text = stringResource(R.string.history_search_placeholder))
                            },
                            singleLine = true,
                            trailingIcon = {
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = onClearQuery) {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            contentDescription = stringResource(
                                                R.string.history_clear_search,
                                            ),
                                        )
                                    }
                                }
                            },
                        )
                    }

                    if (state.isEmpty) {
                        item(key = "empty") {
                            ZdonEmptyState(
                                icon = Icons.Rounded.History,
                                title = stringResource(
                                    if (state.isFiltered) {
                                        R.string.history_empty_filtered_title
                                    } else {
                                        R.string.history_empty_title
                                    },
                                ),
                                description = stringResource(
                                    if (state.isFiltered) {
                                        R.string.history_empty_filtered_description
                                    } else {
                                        R.string.history_empty_description
                                    },
                                ),
                            )
                        }
                    }

                    items(items = state.entries, key = { it.id }) { entry ->
                        HistoryRow(
                            entry = entry,
                            onOpen = { onOpen(entry) },
                            onRedownload = { onRedownload(entry) },
                            onDelete = { onDelete(entry.id) },
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = stringResource(R.string.history_clear_dialog_title)) },
            text = { Text(text = stringResource(R.string.history_clear_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearAll()
                    },
                ) {
                    Text(text = stringResource(R.string.history_clear_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = stringResource(R.string.history_clear_dialog_dismiss))
                }
            },
        )
    }
}

/**
 * Opens a downloaded file with the user's preferred player.
 *
 * The stored path is a SAF content URI, so read permission is granted to the
 * receiving app for the duration of the intent instead of exposing a file path.
 */
private fun openMedia(context: android.content.Context, path: String): Boolean {
    val uri = runCatching { Uri.parse(path) }.getOrNull() ?: return false
    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

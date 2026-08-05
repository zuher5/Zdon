package com.zdon.feature.downloads.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zdon.core.common.util.Formatters
import com.zdon.core.designsystem.component.ZdonInfoChip
import com.zdon.core.designsystem.component.ZdonProgressBar
import com.zdon.core.designsystem.component.ZdonThumbnail
import com.zdon.core.model.DownloadItem
import com.zdon.core.model.DownloadStatus
import com.zdon.feature.downloads.DownloadStrings
import com.zdon.feature.downloads.R

/**
 * One row in the download list: thumbnail, title, live progress and the actions
 * valid for the item's current status.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DownloadRow(
    item: DownloadItem,
    onClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ZdonThumbnail(
                    imageUrl = item.thumbnailUrl,
                    contentDescription = item.title,
                    modifier = Modifier.width(120.dp),
                    durationLabel = Formatters.formatDurationOrNull(item.durationSeconds),
                    cornerRadius = 8,
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.uploader?.let { uploader ->
                        Text(
                            text = uploader,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = stringResource(DownloadStrings.statusLabel(item.status)),
                        style = MaterialTheme.typography.labelMedium,
                        color = item.status.statusColor(),
                    )
                }
            }

            if (item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.PAUSED) {
                val leadingLabel = remember(item.downloadedBytes, item.totalBytes) {
                    progressLeadingLabel(item)
                }
                val trailingLabel = remember(
                    item.speedBytesPerSecond,
                    item.etaSeconds,
                    item.remainingBytes,
                ) {
                    progressTrailingLabel(item)
                }
                ZdonProgressBar(
                    progress = item.progressFraction,
                    indeterminate = item.isIndeterminate,
                    leadingLabel = leadingLabel,
                    trailingLabel = trailingLabel,
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item.currentFragment?.let { fragment ->
                    ZdonInfoChip(label = stringResource(R.string.downloads_fragment, fragment))
                }
                if (item.retryCount > 0) {
                    ZdonInfoChip(
                        label = stringResource(R.string.downloads_retry_count, item.retryCount + 1),
                    )
                }
                item.errorType?.let { errorType ->
                    ZdonInfoChip(
                        label = stringResource(DownloadStrings.errorLabel(errorType)),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.status == DownloadStatus.RUNNING) {
                    IconButton(onClick = onPause) {
                        Icon(
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = stringResource(
                                com.zdon.core.downloader.R.string.action_pause,
                            ),
                        )
                    }
                }
                if (item.status.canResume) {
                    IconButton(onClick = onResume) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(
                                com.zdon.core.downloader.R.string.action_resume,
                            ),
                        )
                    }
                }
                if (item.status.canRetry) {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(
                                com.zdon.core.downloader.R.string.action_retry,
                            ),
                        )
                    }
                }
                if (item.status.canCancel) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = stringResource(
                                com.zdon.core.downloader.R.string.action_cancel,
                            ),
                        )
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.downloads_remove),
                    )
                }
            }
        }
    }
}

private fun progressLeadingLabel(item: DownloadItem): String {
    val downloaded = Formatters.formatBytes(item.downloadedBytes)
    val total = Formatters.formatBytesOrNull(item.totalBytes)
    return if (total != null) {
        "$$downloaded / $total"
    } else {
        downloaded
    }
}

private fun progressTrailingLabel(item: DownloadItem): String {
    val parts = buildList {
        Formatters.formatSpeedOrNull(item.speedBytesPerSecond)?.let(::add)
        if (item.etaSeconds > 0L) {
            add("ETA ${Formatters.formatDuration(item.etaSeconds)}")
        }
        if (item.remainingBytes > 0L) {
            add("${Formatters.formatBytes(item.remainingBytes)} left")
        }
    }
    return parts.joinToString(" · ")
}

@Composable
private fun DownloadStatus.statusColor() = when (this) {
    DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

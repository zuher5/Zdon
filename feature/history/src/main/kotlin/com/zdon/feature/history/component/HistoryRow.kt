package com.zdon.feature.history.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zdon.core.common.util.Formatters
import com.zdon.core.designsystem.component.ZdonThumbnail
import com.zdon.core.model.HistoryEntry
import com.zdon.feature.history.R
import java.text.DateFormat
import java.util.Date

/** One completed download in the history list, separated by a hairline divider. */
@Composable
internal fun HistoryRow(
    entry: HistoryEntry,
    onOpen: () -> Unit,
    onRedownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ZdonThumbnail(
                    imageUrl = entry.thumbnailUrl,
                    contentDescription = entry.title,
                    modifier = Modifier.width(112.dp),
                    durationLabel = Formatters.formatDurationOrNull(entry.durationSeconds),
                    cornerRadius = 8,
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.uploader?.let { uploader ->
                        Text(
                            text = uploader,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = DateFormat
                            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(entry.completedAtMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val caption = buildList {
                Formatters.formatBytesOrNull(entry.fileSizeBytes)?.let(::add)
                if (entry.wasAudioOnly) {
                    add(stringResource(R.string.history_audio_only))
                }
            }
            if (caption.isNotEmpty()) {
                Text(
                    text = caption.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (entry.filePath != null) {
                    IconButton(onClick = onOpen) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = stringResource(R.string.history_open),
                        )
                    }
                }
                IconButton(onClick = onRedownload) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = stringResource(R.string.history_redownload),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.history_delete_entry),
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
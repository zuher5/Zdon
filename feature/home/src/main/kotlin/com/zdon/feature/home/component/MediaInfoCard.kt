package com.zdon.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zdon.core.common.util.Formatters
import com.zdon.core.designsystem.component.ZdonInfoChip
import com.zdon.core.designsystem.component.ZdonThumbnail
import com.zdon.core.model.MediaInfo
import com.zdon.feature.home.R
import androidx.compose.foundation.layout.padding

/**
 * Metadata card shown after a successful analysis: thumbnail, title, uploader,
 * duration, view count and the estimated size of the current selection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MediaInfoCard(
    mediaInfo: MediaInfo,
    estimatedSizeBytes: Long?,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ZdonThumbnail(
                imageUrl = mediaInfo.thumbnailUrl,
                contentDescription = mediaInfo.title,
                modifier = Modifier.fillMaxWidth(),
                durationLabel = Formatters.formatDurationOrNull(mediaInfo.durationSeconds),
            )

            Text(
                text = mediaInfo.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            mediaInfo.uploader?.let { uploader ->
                Text(
                    text = stringResource(R.string.home_uploader, uploader),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Formatters.formatCountOrNull(mediaInfo.viewCount)?.let { views ->
                    ZdonInfoChip(label = stringResource(R.string.home_views, views))
                }
                mediaInfo.extractor?.let { extractor -> ZdonInfoChip(label = extractor) }
                if (mediaInfo.isPlaylist && mediaInfo.entries.isNotEmpty()) {
                    ZdonInfoChip(
                        label = stringResource(
                            R.string.home_playlist_count,
                            mediaInfo.entries.size,
                        ),
                    )
                }
                ZdonInfoChip(
                    label = estimatedSizeBytes
                        ?.let {
                            stringResource(
                                R.string.home_estimated_size,
                                Formatters.formatBytes(it),
                            )
                        }
                        ?: stringResource(R.string.home_size_unknown),
                )
                if (mediaInfo.subtitleLanguages.isNotEmpty()) {
                    ZdonInfoChip(
                        label = pluralStringResource(
                            R.plurals.home_subtitle_languages,
                            mediaInfo.subtitleLanguages.size,
                            mediaInfo.subtitleLanguages.size,
                        ),
                    )
                }
            }
        }
    }
}

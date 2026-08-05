package com.zdon.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zdon.core.common.util.Formatters
import com.zdon.core.designsystem.component.ZdonInfoChip
import com.zdon.core.model.MediaFormat
import com.zdon.feature.home.R

/**
 * One selectable stream. Shows resolution, fps, codec, container, bitrates and
 * the estimated file size so the user can make an informed choice.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FormatRow(
    format: MediaFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = format.primaryLabel(),
                style = MaterialTheme.typography.titleSmall,
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                format.resolution?.let { ZdonInfoChip(label = it) }
                format.fps?.takeIf { it > 0 }?.let {
                    ZdonInfoChip(label = stringResource(R.string.home_fps, it))
                }
                format.extension.takeIf { it.isNotBlank() }?.let { ZdonInfoChip(label = it) }
                format.videoCodec
                    ?.takeIf { format.hasVideo }
                    ?.let { ZdonInfoChip(label = it.substringBefore('.')) }
                format.audioCodec
                    ?.takeIf { format.hasAudio }
                    ?.let { ZdonInfoChip(label = it.substringBefore('.')) }
                format.videoBitrateKbps?.takeIf { it > 0 }?.let {
                    ZdonInfoChip(label = stringResource(R.string.home_video_bitrate, it))
                }
                format.audioBitrateKbps?.takeIf { it > 0 }?.let {
                    ZdonInfoChip(label = stringResource(R.string.home_audio_bitrate, it))
                }
                format.fileSizeBytes?.let { size ->
                    val formatted = Formatters.formatBytes(size)
                    ZdonInfoChip(
                        label = if (format.isApproximateSize) {
                            stringResource(R.string.home_approximate_size, formatted)
                        } else {
                            formatted
                        },
                    )
                }
                if (isSelected) {
                    ZdonInfoChip(
                        label = stringResource(R.string.home_format_selected),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

private fun MediaFormat.primaryLabel(): String = buildString {
    append(formatNote ?: resolution ?: formatId)
    if (isVideoOnly) append(" · video only")
    if (isAudioOnly) append(" · audio only")
}

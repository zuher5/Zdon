package com.zdon.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zdon.core.model.VideoQuality
import com.zdon.feature.home.R

/**
 * Quality picker. Only resolutions the media actually offers are enabled, so the
 * user cannot select a format that would fail during download.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QualitySelector(
    selectedQuality: VideoQuality,
    availableResolutions: List<Int>,
    hasAudioFormats: Boolean,
    onQualitySelected: (VideoQuality) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.home_quality_title),
            style = MaterialTheme.typography.titleSmall,
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoQuality.selectableDefaults.forEach { quality ->
                val enabled = when {
                    quality == VideoQuality.BEST -> true
                    quality == VideoQuality.AUDIO_ONLY -> hasAudioFormats
                    availableResolutions.isEmpty() -> true
                    else -> availableResolutions.any { it >= (quality.maxHeight ?: 0) } ||
                        availableResolutions.any { it == quality.maxHeight }
                }

                FilterChip(
                    selected = selectedQuality == quality,
                    onClick = { onQualitySelected(quality) },
                    enabled = enabled,
                    label = { Text(text = quality.label) },
                )
            }
        }
    }
}

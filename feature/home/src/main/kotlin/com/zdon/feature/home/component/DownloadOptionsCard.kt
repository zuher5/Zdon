package com.zdon.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import com.zdon.core.model.AudioFormat
import com.zdon.feature.home.HomeUiState
import com.zdon.feature.home.R

/** Post-processing and output options for the pending download. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DownloadOptionsCard(
    state: HomeUiState,
    onExtractAudioChange: (Boolean) -> Unit,
    onAudioFormatSelected: (AudioFormat) -> Unit,
    onDownloadSubtitlesChange: (Boolean) -> Unit,
    onEmbedThumbnailChange: (Boolean) -> Unit,
    onEmbedMetadataChange: (Boolean) -> Unit,
    onDownloadPlaylistChange: (Boolean) -> Unit,
    onCustomFileNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.home_options_title),
                style = MaterialTheme.typography.titleSmall,
            )

            OptionSwitch(
                label = stringResource(R.string.home_extract_audio),
                checked = state.extractAudio,
                onCheckedChange = onExtractAudioChange,
            )

            if (state.extractAudio || state.selectedQuality.isAudioOnly) {
                Text(
                    text = stringResource(R.string.home_audio_format),
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AudioFormat.entries.forEach { format ->
                        FilterChip(
                            selected = state.audioFormat == format,
                            onClick = { onAudioFormatSelected(format) },
                            label = { Text(text = format.label) },
                        )
                    }
                }
            }

            HorizontalDivider()

            OptionSwitch(
                label = stringResource(R.string.home_download_subtitles),
                checked = state.downloadSubtitles,
                onCheckedChange = onDownloadSubtitlesChange,
            )
            OptionSwitch(
                label = stringResource(R.string.home_embed_thumbnail),
                checked = state.embedThumbnail,
                onCheckedChange = onEmbedThumbnailChange,
            )
            OptionSwitch(
                label = stringResource(R.string.home_embed_metadata),
                checked = state.embedMetadata,
                onCheckedChange = onEmbedMetadataChange,
            )

            if (state.mediaInfo?.isPlaylist == true) {
                OptionSwitch(
                    label = stringResource(R.string.home_download_playlist),
                    checked = state.downloadPlaylist,
                    onCheckedChange = onDownloadPlaylistChange,
                )
            }

            OutlinedTextField(
                value = state.customFileName,
                onValueChange = onCustomFileNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.home_custom_filename)) },
                supportingText = {
                    Text(text = stringResource(R.string.home_custom_filename_supporting))
                },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun OptionSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

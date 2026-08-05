package com.zdon.feature.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.zdon.feature.home.R
import com.zdon.feature.home.UrlInputError

/**
 * Large URL entry field with paste, clear and analyse affordances.
 *
 * The keyboard is configured for URI entry with a search action so a hardware or
 * on-screen enter key triggers analysis directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UrlInputCard(
    url: String,
    error: UrlInputError?,
    isAnalyzing: Boolean,
    canAnalyze: Boolean,
    onUrlChange: (String) -> Unit,
    onPasteClick: () -> Unit,
    onClearClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.home_url_label)) },
            placeholder = { Text(text = stringResource(R.string.home_url_placeholder)) },
            supportingText = {
                val message = error?.messageRes()?.let { stringResource(it) }
                Text(text = message ?: stringResource(R.string.home_url_supporting))
            },
            isError = error != null,
            minLines = 2,
            maxLines = 4,
            trailingIcon = {
                if (url.isNotEmpty()) {
                    IconButton(onClick = onClearClick) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = stringResource(R.string.home_clear_url),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(onSearch = { if (canAnalyze) onAnalyzeClick() }),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onPasteClick,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentPaste,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(R.string.home_paste),
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                    softWrap = false,
                )
            }

            OutlinedButton(
                onClick = onAnalyzeClick,
                enabled = canAnalyze && !isAnalyzing,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(R.string.home_analyze),
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Button(
                onClick = onDownloadClick,
                enabled = canAnalyze && !isAnalyzing,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.home_download),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

private fun UrlInputError.messageRes(): Int = when (this) {
    UrlInputError.MALFORMED -> R.string.home_error_malformed_url
    UrlInputError.UNSUPPORTED_SCHEME -> R.string.home_error_unsupported_scheme
    UrlInputError.ILLEGAL_CHARACTERS -> R.string.home_error_illegal_characters
}

package com.zdon.feature.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zdon.core.designsystem.util.isExpandedWidth
import com.zdon.core.model.AudioFormat
import com.zdon.core.model.ThemeMode
import com.zdon.core.model.UserPreferences
import com.zdon.core.model.VideoQuality
import com.zdon.feature.settings.component.SettingsChoiceDialog
import com.zdon.feature.settings.component.SettingsInfoRow
import com.zdon.feature.settings.component.SettingsSectionHeader
import com.zdon.feature.settings.component.SettingsSliderDialog
import com.zdon.feature.settings.component.SettingsSwitchRow
import com.zdon.feature.settings.component.SettingsTextDialog
import com.zdon.feature.settings.component.SettingsValueRow

/**
 * Settings route.
 *
 * Owns the two SAF launchers (folder tree and cookies document) because
 * `rememberLauncherForActivityResult` must be called from a composable; the
 * resulting URI is handed straight to the ViewModel.
 */
@Composable
fun SettingsRoute(
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? -> uri?.let { viewModel.onDownloadFolderSelected(it.toString()) } }

    val cookiesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist read access so the cookies file remains usable after reboot.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onCookiesFileSelected(uri.toString())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onNotificationsBlocked(
            !NotificationManagerCompat.from(context).areNotificationsEnabled(),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> onShowMessage(context.getString(event.messageRes))
                is SettingsEvent.ShowRawMessage -> onShowMessage(event.message)
            }
        }
    }

    SettingsScreen(
        state = state,
        modifier = modifier,
        onChooseFolder = { folderLauncher.launch(null) },
        onChooseCookies = { cookiesLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
        onClearCookies = { viewModel.onCookiesFileSelected(null) },
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onDynamicColorChanged = viewModel::onDynamicColorChanged,
        onConcurrentChanged = viewModel::onConcurrentDownloadsChanged,
        onRetriesChanged = viewModel::onMaxRetriesChanged,
        onDefaultQualitySelected = viewModel::onDefaultQualitySelected,
        onDefaultAudioFormatSelected = viewModel::onDefaultAudioFormatSelected,
        onAutoUpdateYtDlpChanged = viewModel::onAutoUpdateYtDlpChanged,
        onAutoUpdateFfmpegChanged = viewModel::onAutoUpdateFfmpegChanged,
        onNotificationsChanged = viewModel::onNotificationsChanged,
        onOpenSystemNotificationSettings = { context.openNotificationSettings() },
        onProxyChanged = viewModel::onProxyChanged,
        onHeadersChanged = viewModel::onCustomHeadersChanged,
        onOutputTemplateChanged = viewModel::onOutputTemplateChanged,
        onEmbedMetadataChanged = viewModel::onEmbedMetadataChanged,
        onEmbedThumbnailChanged = viewModel::onEmbedThumbnailChanged,
        onDownloadSubtitlesChanged = viewModel::onDownloadSubtitlesChanged,
        onSubtitleLanguagesChanged = viewModel::onSubtitleLanguagesChanged,
        onDownloadArchiveChanged = viewModel::onDownloadArchiveChanged,
        onAutoResumeAfterBootChanged = viewModel::onAutoResumeAfterBootChanged,
        onRestrictFilenamesChanged = viewModel::onRestrictFilenamesChanged,
        onClearRecentUrls = viewModel::onClearRecentUrls,
        onUpdateNow = viewModel::updateYtDlpNow,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onChooseFolder: () -> Unit,
    onChooseCookies: () -> Unit,
    onClearCookies: () -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onConcurrentChanged: (Int) -> Unit,
    onRetriesChanged: (Int) -> Unit,
    onDefaultQualitySelected: (VideoQuality) -> Unit,
    onDefaultAudioFormatSelected: (AudioFormat) -> Unit,
    onAutoUpdateYtDlpChanged: (Boolean) -> Unit,
    onAutoUpdateFfmpegChanged: (Boolean) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onOpenSystemNotificationSettings: () -> Unit,
    onProxyChanged: (String) -> Unit,
    onHeadersChanged: (String) -> Unit,
    onOutputTemplateChanged: (String) -> Unit,
    onEmbedMetadataChanged: (Boolean) -> Unit,
    onEmbedThumbnailChanged: (Boolean) -> Unit,
    onDownloadSubtitlesChanged: (Boolean) -> Unit,
    onSubtitleLanguagesChanged: (String) -> Unit,
    onDownloadArchiveChanged: (Boolean) -> Unit,
    onAutoResumeAfterBootChanged: (Boolean) -> Unit,
    onRestrictFilenamesChanged: (Boolean) -> Unit,
    onClearRecentUrls: () -> Unit,
    onUpdateNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeDialog by remember { mutableStateOf(SettingsDialog.NONE) }
    val preferences = state.preferences
    val horizontalPadding = if (isExpandedWidth()) 32.dp else 16.dp

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(text = stringResource(R.string.settings_title)) }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(key = "appearance-header") {
                SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
            }
            item(key = "theme") {
                SettingsValueRow(
                    title = stringResource(R.string.settings_theme),
                    value = preferences.themeMode.label,
                    onClick = { activeDialog = SettingsDialog.THEME },
                )
            }
            item(key = "dynamic-color") {
                val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_dynamic_color),
                    summary = stringResource(
                        if (supported) {
                            R.string.settings_dynamic_color_summary
                        } else {
                            R.string.settings_dynamic_color_unsupported
                        },
                    ),
                    checked = preferences.useDynamicColor && supported,
                    onCheckedChange = onDynamicColorChanged,
                    enabled = supported,
                )
            }

            item(key = "storage-header") {
                SettingsSectionHeader(stringResource(R.string.settings_section_storage))
            }
            item(key = "folder") {
                SettingsValueRow(
                    title = stringResource(R.string.settings_download_folder),
                    value = preferences.downloadDirectoryLabel
                        ?: stringResource(R.string.settings_download_folder_none),
                    summary = stringResource(R.string.settings_download_folder_summary),
                    onClick = onChooseFolder,
                )
            }

            item(key = "downloads-header") {
                SettingsSectionHeader(stringResource(R.string.settings_section_downloads))
            }
            item(key = "concurrent") {
                SettingsValueRow(
                    title = stringResource(R.string.settings_concurrent),
                    value = stringResource(
                        R.string.settings_concurrent_value,
                        preferences.maxConcurrentDownloads,
                    ),
                    onClick = { activeDialog = SettingsDialog.CONCURRENT },
                )
            }
            item(key = "retries") {
                SettingsValueRow(
                    title = stringResource(R.string.settings_retries),
                    value = stringResource(R.string.settings_retries_value, preferences.maxRetries),
                    onClick = { activeDialog = SettingsDialog.RETRIES },
                )
            }
            item(key = "auto-resume") {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_auto_resume),
                    summary = stringResource(R.string.settings_auto_resume_summary),
                    checked = preferences.autoResumeAfterBoot,
                    onCheckedChange = onAutoResumeAfterBootChanged,
                )
            }
            item(key = "default-quality") {
                SettingsValueRow(
                    title = stringResource(R.string.settings_default_quality),
                    value = preferences.defaultQuality.label,
                    onClick = { activeDialog = SettingsDialog.QUALITY },
                )
            }
            item(key = "default-audio") {
                SettingsValueRow(
                    title = stringResource(R.string.settings_default_audio_format),
                    value = preferences.defaultAudioFormat.label,
                    onClick = { activeDialog = SettingsDialog.AUDIO_FORMAT },
                )
            }
            item(key = "archive") {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_download_archive),
                    summary = stringResource(R.string.settings_download_archive_summary),
                    checked = preferences.useDownloadArchive,
                    onCheckedChange = onDownloadArchiveChanged,
                )
            }

            item(key = "output-header") {
                SettingsSectionHeader(stringResource(R.string.settings_section_output))
            }
            item(key = "template") {
                SettingsValueRow(
                    title = stringResource(R.string.settings_output_template),
                    value = preferences.outputTemplate,
                    summary = stringResource(R.string.settings_output_template_summary),
                    onClick = { activeDialog = SettingsDialog.OUTPUT_TEMPLATE },
                )
            }
            item(key = "restrict") {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_restrict_filenames),
                    summary = stringResource(R.string.settings_restrict_filenames_summary),
                    checked = preferences.restrictFilenames,
                    onCheckedChange = onRestrictFilenamesChanged,
                )
            }
            item(key = "metadata") {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_embed_metadata),
                    checked = preferences.embedMetadata,
                    onCheckedChange = onEmbedMetadataChanged,
                )
            }
            item(key = "thumbnail") {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_embed_thumbnail),
                    checked = preferences.embedThumbnail,
                    onCheckedChange = onEmbedThumbnailChanged,
                )
            }
            item(key = "subtitles") {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_download_subtitles),
                    checked = preferences.downloadSubtitles,
                    onCheckedChange = onDownloadSubtitlesChanged,
                )
            }
            item(key = "subtitle-languages") {
                SettingsValueRow(
                    title = stringResource(R.string.settings_subtitle_languages),
                    value = preferences.subtitleLanguages,
                    summary = stringResource(R.string.settings_subtitle_languages_summary),
                    onClick = { activeDialog = SettingsDialog.SUBTITLE_LANGUAGES },
                )
            }

            item(key = "network-header") {
                SettingsSectionHeader(stringResource(R.string.settings_section_network))
            }
            item(key = "proxy") {
                SettingsValueRow(
                    title = stringResource(R.string.settings_proxy),
                    value = preferences.proxyUrl
                        ?: stringResource(R.string.settings_download_folder_none),
                    summary = stringResource(R.string.settings_proxy_summary),
                    onClick = { activeDialog = SettingsDialog.PROXY },
                )
            }
            item(key = "cookies") {
                SettingsValueRow(
                    title = stringResource(R.string.settings_cookies),
                    value = if (preferences.cookiesFileUri != null) {
                        "Imported"
                    } else {
                        stringResource(R.string.settings_cookies_none)
                    },
                    summary = stringResource(R.string.settings_cookies_summary),
                    onClick = onChooseCookies,
                )
            }
            if (preferences.cookiesFileUri != null) {
                item(key = "cookies-clear") {
                    TextButton(
                        onClick = onClearCookies,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_cookies_clear),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
            item(key = "headers") {
                SettingsValueRow(
                    title = stringResource(R.string.settings_headers),
                    value = preferences.customHttpHeaders
                        ?: stringResource(R.string.settings_download_folder_none),
                    summary = stringResource(R.string.settings_headers_summary),
                    onClick = { activeDialog = SettingsDialog.HEADERS },
                )
            }

            item(key = "notifications-header") {
                SettingsSectionHeader(stringResource(R.string.settings_section_notifications))
            }
            item(key = "notifications") {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_notifications),
                    summary = stringResource(R.string.settings_notifications_summary),
                    checked = preferences.notificationsEnabled,
                    onCheckedChange = onNotificationsChanged,
                )
            }
            if (state.notificationsBlockedBySystem) {
                item(key = "notifications-blocked") {
                    Text(
                        text = stringResource(R.string.settings_notifications_blocked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onOpenSystemNotificationSettings) {
                        Text(
                            text = stringResource(R.string.settings_open_system_settings),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }

            item(key = "engine-header") {
                SettingsSectionHeader(stringResource(R.string.settings_section_engine))
            }
            item(key = "ytdlp-version") {
                SettingsInfoRow(
                    title = stringResource(R.string.settings_ytdlp_version),
                    value = state.engineStatus.ytDlpVersion
                        ?: stringResource(R.string.settings_ytdlp_version_unknown),
                )
            }
            item(key = "ffmpeg-status") {
                SettingsInfoRow(
                    title = stringResource(R.string.settings_ffmpeg_status),
                    value = stringResource(
                        if (state.engineStatus.isFfmpegAvailable) {
                            R.string.settings_ffmpeg_available
                        } else {
                            R.string.settings_ffmpeg_unavailable
                        },
                    ),
                )
            }
            state.engineStatus.initializationError?.let { error ->
                item(key = "engine-error") {
                    Text(
                        text = stringResource(R.string.settings_engine_error, error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item(key = "auto-update-ytdlp") {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_auto_update_ytdlp),
                    summary = stringResource(R.string.settings_auto_update_ytdlp_summary),
                    checked = preferences.autoUpdateYtDlp,
                    onCheckedChange = onAutoUpdateYtDlpChanged,
                )
            }
            item(key = "auto-update-ffmpeg") {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_auto_update_ffmpeg),
                    checked = preferences.autoUpdateFfmpeg,
                    onCheckedChange = onAutoUpdateFfmpegChanged,
                )
            }
            item(key = "update-now") {
                Button(
                    onClick = onUpdateNow,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isUpdatingBinary,
                ) {
                    Text(
                        text = stringResource(
                            if (state.isUpdatingBinary) {
                                R.string.settings_updating
                            } else {
                                R.string.settings_update_now
                            },
                        ),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            item(key = "clear-recent") {
                TextButton(onClick = onClearRecentUrls) {
                    Text(
                        text = stringResource(R.string.settings_clear_recent),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }

    when (activeDialog) {
        SettingsDialog.NONE -> Unit

        SettingsDialog.THEME -> SettingsChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = preferences.themeMode,
            optionLabel = ThemeMode::label,
            onSelect = onThemeModeSelected,
            onDismiss = { activeDialog = SettingsDialog.NONE },
        )

        SettingsDialog.QUALITY -> SettingsChoiceDialog(
            title = stringResource(R.string.settings_default_quality),
            options = VideoQuality.selectableDefaults,
            selected = preferences.defaultQuality,
            optionLabel = VideoQuality::label,
            onSelect = onDefaultQualitySelected,
            onDismiss = { activeDialog = SettingsDialog.NONE },
        )

        SettingsDialog.AUDIO_FORMAT -> SettingsChoiceDialog(
            title = stringResource(R.string.settings_default_audio_format),
            options = AudioFormat.entries,
            selected = preferences.defaultAudioFormat,
            optionLabel = AudioFormat::label,
            onSelect = onDefaultAudioFormatSelected,
            onDismiss = { activeDialog = SettingsDialog.NONE },
        )

        SettingsDialog.CONCURRENT -> SettingsSliderDialog(
            title = stringResource(R.string.settings_concurrent),
            initialValue = preferences.maxConcurrentDownloads,
            valueRange = UserPreferences.MIN_CONCURRENT_DOWNLOADS..
                UserPreferences.MAX_CONCURRENT_DOWNLOADS,
            valueLabel = { stringResource(R.string.settings_concurrent_value, it) },
            onConfirm = onConcurrentChanged,
            onDismiss = { activeDialog = SettingsDialog.NONE },
        )

        SettingsDialog.RETRIES -> SettingsSliderDialog(
            title = stringResource(R.string.settings_retries),
            initialValue = preferences.maxRetries,
            valueRange = UserPreferences.MIN_MAX_RETRIES..UserPreferences.MAX_MAX_RETRIES,
            valueLabel = { stringResource(R.string.settings_retries_value, it) },
            onConfirm = onRetriesChanged,
            onDismiss = { activeDialog = SettingsDialog.NONE },
        )

        SettingsDialog.OUTPUT_TEMPLATE -> SettingsTextDialog(
            title = stringResource(R.string.settings_output_template),
            initialValue = preferences.outputTemplate,
            supportingText = stringResource(R.string.settings_output_template_summary),
            singleLine = true,
            onConfirm = onOutputTemplateChanged,
            onDismiss = { activeDialog = SettingsDialog.NONE },
        )

        SettingsDialog.SUBTITLE_LANGUAGES -> SettingsTextDialog(
            title = stringResource(R.string.settings_subtitle_languages),
            initialValue = preferences.subtitleLanguages,
            supportingText = stringResource(R.string.settings_subtitle_languages_summary),
            singleLine = true,
            onConfirm = onSubtitleLanguagesChanged,
            onDismiss = { activeDialog = SettingsDialog.NONE },
        )

        SettingsDialog.PROXY -> SettingsTextDialog(
            title = stringResource(R.string.settings_proxy),
            initialValue = preferences.proxyUrl.orEmpty(),
            supportingText = stringResource(R.string.settings_proxy_summary),
            singleLine = true,
            onConfirm = onProxyChanged,
            onDismiss = { activeDialog = SettingsDialog.NONE },
        )

        SettingsDialog.HEADERS -> SettingsTextDialog(
            title = stringResource(R.string.settings_headers),
            initialValue = preferences.customHttpHeaders.orEmpty(),
            supportingText = stringResource(R.string.settings_headers_summary),
            singleLine = false,
            onConfirm = onHeadersChanged,
            onDismiss = { activeDialog = SettingsDialog.NONE },
        )
    }
}

/** Which settings dialog is currently open. */
internal enum class SettingsDialog {
    NONE,
    THEME,
    QUALITY,
    AUDIO_FORMAT,
    CONCURRENT,
    RETRIES,
    OUTPUT_TEMPLATE,
    SUBTITLE_LANGUAGES,
    PROXY,
    HEADERS,
}

/** Opens the per-app notification settings page. */
private fun android.content.Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }
}

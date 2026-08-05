package com.zdon.app

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zdon.app.ui.ZdonApp
import com.zdon.core.designsystem.theme.ZdonTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host.
 *
 * The splash screen is held until the theme preference has been read, so the app
 * never flashes the wrong colour scheme. The notification permission is requested
 * once on Android 13+, and declining it does not block downloads.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Downloads work regardless; only the progress notification is affected. */ }

    private val downloadFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? -> uri?.let { viewModel.onDownloadFolderSelected(it.toString()) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var uiState: MainUiState = MainUiState.Loading
        splashScreen.setKeepOnScreenCondition { uiState.shouldKeepSplashScreen }

        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            uiState = state

            ZdonTheme(
                themeMode = state.themeModeOrDefault,
                useDynamicColor = state.dynamicColorOrDefault,
            ) {
                ZdonApp(
                    activeDownloadCount = state.activeCountOrZero,
                    onChooseFolder = { downloadFolderLauncher.launch(null) },
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

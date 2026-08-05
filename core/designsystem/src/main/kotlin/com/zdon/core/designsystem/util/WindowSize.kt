package com.zdon.core.designsystem.util

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Responsive layout helper.
 *
 * The window size class is derived from the current configuration rather than
 * from the Activity, which keeps the design system module free of an Activity
 * dependency and works correctly inside previews and in multi-window mode.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberZdonWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val size = DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
    return remember(size) { WindowSizeClass.calculateFromSize(size) }
}

/** True for tablets and unfolded foldables, where a two-pane layout fits. */
@Composable
fun isExpandedWidth(): Boolean =
    rememberZdonWindowSizeClass().widthSizeClass != WindowWidthSizeClass.Compact

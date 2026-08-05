package com.zdon.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.zdon.app.R

/**
 * Top-level destinations shown in the navigation bar.
 *
 * Routes are plain constants rather than sealed-class serialisation so the graph
 * stays readable and a deep link can be built by string concatenation.
 */
enum class TopLevelDestination(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelRes: Int,
) {
    HOME(
        route = ZdonRoutes.HOME,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        labelRes = R.string.destination_home,
    ),
    DOWNLOADS(
        route = ZdonRoutes.DOWNLOADS,
        selectedIcon = Icons.Rounded.Download,
        unselectedIcon = Icons.Outlined.Download,
        labelRes = R.string.destination_downloads,
    ),
    HISTORY(
        route = ZdonRoutes.HISTORY,
        selectedIcon = Icons.Rounded.History,
        unselectedIcon = Icons.Outlined.History,
        labelRes = R.string.destination_history,
    ),
    SETTINGS(
        route = ZdonRoutes.SETTINGS,
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        labelRes = R.string.destination_settings,
    ),
}

/** All navigation routes in the app. */
object ZdonRoutes {
    const val HOME = "home"
    const val DOWNLOADS = "downloads"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    /** Detail screen for a single download. */
    const val DOWNLOAD_DETAIL_ARG_ID = "downloadId"
    const val DOWNLOAD_DETAIL = "download_detail/{$DOWNLOAD_DETAIL_ARG_ID}"

    fun downloadDetail(downloadId: Long): String = "download_detail/$downloadId"
}

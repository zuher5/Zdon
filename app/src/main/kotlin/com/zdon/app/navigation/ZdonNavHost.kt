package com.zdon.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zdon.feature.downloads.DownloadDetailRoute
import com.zdon.feature.downloads.DownloadsRoute
import com.zdon.feature.history.HistoryRoute
import com.zdon.feature.home.HomeRoute
import com.zdon.feature.settings.SettingsRoute

/**
 * Application navigation graph.
 *
 * Every destination is a route composable that owns its own ViewModel, so the
 * graph itself holds no state and back-stack restoration works without extra
 * plumbing.
 */
@Composable
fun ZdonNavHost(
    navController: NavHostController,
    onChooseFolder: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialSharedUrl: String? = null,
) {
    NavHost(
        navController = navController,
        startDestination = ZdonRoutes.HOME,
        modifier = modifier,
    ) {
        composable(route = ZdonRoutes.HOME) {
            HomeRoute(
                initialSharedUrl = initialSharedUrl,
                onChooseFolder = onChooseFolder,
                onShowMessage = onShowMessage,
            )
        }

        composable(route = ZdonRoutes.DOWNLOADS) {
            DownloadsRoute(
                onDownloadClick = { downloadId ->
                    navController.navigate(ZdonRoutes.downloadDetail(downloadId))
                },
            )
        }

        composable(route = ZdonRoutes.HISTORY) {
            HistoryRoute(onShowMessage = onShowMessage)
        }

        composable(route = ZdonRoutes.SETTINGS) {
            SettingsRoute(onShowMessage = onShowMessage)
        }

        composable(
            route = ZdonRoutes.DOWNLOAD_DETAIL,
            arguments = listOf(
                navArgument(ZdonRoutes.DOWNLOAD_DETAIL_ARG_ID) { type = NavType.LongType },
            ),
        ) {
            DownloadDetailRoute(onBackClick = navController::popBackStack)
        }
    }
}

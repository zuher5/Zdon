package com.zdon.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zdon.app.navigation.TopLevelDestination
import com.zdon.app.navigation.ZdonNavHost
import com.zdon.app.navigation.ZdonRoutes
import com.zdon.core.designsystem.util.isExpandedWidth
import kotlinx.coroutines.launch

/**
 * App shell: navigation bar on phones, navigation rail on expanded widths, plus a
 * single snackbar host shared by every screen.
 */
@Composable
fun ZdonApp(
    activeDownloadCount: Int,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    initialSharedUrl: String? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showNavigation = currentRoute in TOP_LEVEL_ROUTES
    val expanded = isExpandedWidth()

    // Snackbars are shown from a scope tied to this composition, so a message
    // queued while navigating away is cancelled instead of leaking.
    val showMessage: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showNavigation && !expanded) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = backStackEntry.isSelected(destination)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTopLevel(destination) },
                            icon = {
                                DestinationIcon(
                                    destination = destination,
                                    selected = selected,
                                    badgeCount = destination.badgeCount(activeDownloadCount),
                                )
                            },
                            label = { Text(text = stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Row(modifier = Modifier.padding(innerPadding)) {
            if (showNavigation && expanded) {
                NavigationRail {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = backStackEntry.isSelected(destination)
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navController.navigateToTopLevel(destination) },
                            icon = {
                                DestinationIcon(
                                    destination = destination,
                                    selected = selected,
                                    badgeCount = destination.badgeCount(activeDownloadCount),
                                )
                            },
                            label = { Text(text = stringResource(destination.labelRes)) },
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                ZdonNavHost(
                    navController = navController,
                    initialSharedUrl = initialSharedUrl,
                    onChooseFolder = onChooseFolder,
                    onShowMessage = showMessage,
                )
            }
        }
    }
}

@Composable
private fun DestinationIcon(
    destination: TopLevelDestination,
    selected: Boolean,
    badgeCount: Int,
) {
    val icon = if (selected) destination.selectedIcon else destination.unselectedIcon
    if (badgeCount > 0) {
        BadgedBox(badge = { Badge { Text(text = badgeCount.toString()) } }) {
            Icon(imageVector = icon, contentDescription = null)
        }
    } else {
        Icon(imageVector = icon, contentDescription = null)
    }
}

private fun TopLevelDestination.badgeCount(activeDownloadCount: Int): Int =
    if (this == TopLevelDestination.DOWNLOADS) activeDownloadCount else 0

/**
 * Navigates to a top-level destination, keeping a single instance on the stack and
 * restoring that destination's previous state.
 */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavBackStackEntry?.isSelected(destination: TopLevelDestination): Boolean =
    this?.destination?.hierarchy?.any { it.route == destination.route } == true

private val TOP_LEVEL_ROUTES = setOf(
    ZdonRoutes.HOME,
    ZdonRoutes.DOWNLOADS,
    ZdonRoutes.HISTORY,
    ZdonRoutes.SETTINGS,
)

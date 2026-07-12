package com.photocleaner.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.photocleaner.R
import com.photocleaner.ui.home.HomeScreen
import com.photocleaner.ui.review.ReviewScreen
import com.photocleaner.ui.scan.ScanScreen
import com.photocleaner.ui.settings.SettingsScreen
import com.photocleaner.ui.stats.StatsScreen
import com.photocleaner.util.MediaAccessLevel
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ScanRoute
@Serializable data object ReviewRoute
@Serializable data object StatsRoute
@Serializable data object SettingsRoute

private data class Screen(val route: Any, val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun NavGraph(
    snackbarHostState: SnackbarHostState,
    mediaAccessLevel: MediaAccessLevel,
    onMissingPermission: () -> Unit
) {
    val navController = rememberNavController()
    val screens = listOf(
        Screen(HomeRoute, R.string.nav_home, Icons.Default.Home),
        Screen(ScanRoute, R.string.nav_scan, Icons.Default.CameraAlt),
        Screen(ReviewRoute, R.string.nav_review, Icons.Default.Preview),
        Screen(StatsRoute, R.string.nav_stats, Icons.Default.BarChart),
        Screen(SettingsRoute, R.string.nav_settings, Icons.Default.Settings)
    )
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            screens.forEach { screen ->
                item(
                    icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                    label = { Text(stringResource(screen.titleRes)) },
                    selected = currentDestination?.hierarchy?.any { it.hasRoute(screen.route::class) } == true,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.fillMaxSize().padding(innerPadding).statusBarsPadding()
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        onNavigateToScan = { navController.navigate(ScanRoute) },
                        onNavigateToReview = { navController.navigate(ReviewRoute) },
                        onNavigateToStats = { navController.navigate(StatsRoute) },
                        onNavigateToSettings = { navController.navigate(SettingsRoute) }
                    )
                }
                composable<ScanRoute> { ScanScreen(snackbarHostState, mediaAccessLevel, onMissingPermission) }
                composable<ReviewRoute> { ReviewScreen(snackbarHostState) }
                composable<StatsRoute> { StatsScreen() }
                composable<SettingsRoute> { SettingsScreen(mediaAccessLevel, onMissingPermission) }
            }
        }
    }
}

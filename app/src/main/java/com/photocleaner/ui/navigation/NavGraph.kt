package com.photocleaner.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.photocleaner.R
import com.photocleaner.ui.home.HomeScreen
import com.photocleaner.ui.scan.ScanScreen
import com.photocleaner.ui.settings.SettingsScreen
import com.photocleaner.ui.stats.StatsScreen
import com.photocleaner.ui.review.ReviewScreen
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object ScanRoute
@Serializable data object ReviewRoute
@Serializable data object StatsRoute
@Serializable data object SettingsRoute

sealed class Screen(val route: Any, val titleRes: Int, val icon: ImageVector) {
    data object Home : Screen(HomeRoute, R.string.nav_home, Icons.Default.Home)
    data object Scan : Screen(ScanRoute, R.string.nav_scan, Icons.Default.CameraAlt)
    data object Review : Screen(ReviewRoute, R.string.nav_review, Icons.Default.Preview)
    data object Stats : Screen(StatsRoute, R.string.nav_stats, Icons.Default.BarChart)
    data object Settings : Screen(SettingsRoute, R.string.nav_settings, Icons.Default.Info)
}

@Composable
fun NavGraph(
    snackbarHostState: SnackbarHostState,
    onMissingPermission: () -> Unit
) {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Scan, Screen.Review, Screen.Stats, Screen.Settings)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.titleRes)) },
                        selected = currentDestination?.hierarchy?.any { it.hasRoute(screen.route::class) } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onNavigateToScan = { navController.navigate(ScanRoute) },
                    onNavigateToReview = { navController.navigate(ReviewRoute) },
                    onNavigateToStats = { navController.navigate(StatsRoute) },
                    onNavigateToSettings = { navController.navigate(SettingsRoute) }
                )
            }
            composable<ScanRoute> {
                ScanScreen(
                    snackbarHostState = snackbarHostState,
                    onRequestPermission = onMissingPermission
                )
            }
            composable<ReviewRoute> {
                ReviewScreen(
                    snackbarHostState = snackbarHostState
                )
            }
            composable<StatsRoute> { StatsScreen() }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}

package com.photocleaner.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import com.photocleaner.ui.home.HomeScreen
import com.photocleaner.ui.scan.ScanScreen
import com.photocleaner.ui.review.ReviewScreen
import com.photocleaner.ui.stats.StatsScreen
import com.photocleaner.ui.settings.SettingsScreen

@Serializable data object HomeRoute
@Serializable data object ScanRoute
@Serializable data object ReviewRoute
@Serializable data object StatsRoute
@Serializable data object SettingsRoute

sealed class Screen(val route: Any, val title: String, val icon: ImageVector) {
    data object Home : Screen(HomeRoute, "\u9996\u9875", Icons.Default.Home)
    data object Scan : Screen(ScanRoute, "\u626b\u63cf", Icons.Default.CameraAlt)
    data object Review : Screen(ReviewRoute, "\u5ba1\u67e5", Icons.Default.Preview)
    data object Stats : Screen(StatsRoute, "\u7edf\u8ba1", Icons.Default.BarChart)
    data object Settings : Screen(SettingsRoute, "\u8bbe\u7f6e", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Scan, Screen.Review, Screen.Stats, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
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
            composable<ScanRoute> { ScanScreen() }
            composable<ReviewRoute> { ReviewScreen() }
            composable<StatsRoute> { StatsScreen() }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}

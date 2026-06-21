package com.photocleaner.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.photocleaner.ui.home.HomeScreen
import com.photocleaner.ui.scan.ScanScreen
import com.photocleaner.ui.review.ReviewScreen
import com.photocleaner.ui.stats.StatsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "\u9996\u9875", Icons.Default.Home)
    data object Scan : Screen("scan", "\u626b\u63cf", Icons.Default.CameraAlt)
    data object Review : Screen("review", "\u5ba1\u67e5", Icons.Default.Preview)
    data object Stats : Screen("stats", "\u7edf\u8ba1", Icons.Default.BarChart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Home, Screen.Scan, Screen.Review, Screen.Stats)

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
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                    onNavigateToReview = { navController.navigate(Screen.Review.route) },
                    onNavigateToStats = { navController.navigate(Screen.Stats.route) }
                )
            }
            composable(Screen.Scan.route) { ScanScreen() }
            composable(Screen.Review.route) { ReviewScreen() }
            composable(Screen.Stats.route) { StatsScreen() }
        }
    }
}

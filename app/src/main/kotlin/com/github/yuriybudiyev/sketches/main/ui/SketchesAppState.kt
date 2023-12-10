package com.github.yuriybudiyev.sketches.main.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.github.yuriybudiyev.sketches.buckets.navigation.BUCKETS_ROUTE
import com.github.yuriybudiyev.sketches.buckets.navigation.navigateToBuckets
import com.github.yuriybudiyev.sketches.images.navigation.IMAGES_ROUTE
import com.github.yuriybudiyev.sketches.images.navigation.navigateToImages
import com.github.yuriybudiyev.sketches.main.navigation.TopLevelDestination
import com.github.yuriybudiyev.sketches.main.navigation.TopLevelNavigation

@Composable
fun rememberSketchesAppState(
    windowSizeClass: WindowSizeClass,
    navController: NavHostController = rememberNavController()
): SketchesAppState =
    remember(
        windowSizeClass,
        navController
    ) {
        SketchesAppState(
            windowSizeClass,
            navController
        )
    }

@Stable
class SketchesAppState(
    val windowSizeClass: WindowSizeClass,
    val navController: NavHostController
) {

    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() = when (currentDestination?.route) {
            IMAGES_ROUTE -> TopLevelDestination.IMAGES
            BUCKETS_ROUTE -> TopLevelDestination.BUCKETS
            else -> null
        }

    val currentTopLevelNavigation: TopLevelNavigation
        @Composable get() = when (currentTopLevelDestination) {
            TopLevelDestination.IMAGES, TopLevelDestination.BUCKETS -> {
                if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                    TopLevelNavigation.BAR
                } else {
                    TopLevelNavigation.RAIL
                }
            }
            else -> {
                TopLevelNavigation.NONE
            }
        }

    val topLevelDestinations: List<TopLevelDestination>
        get() = TopLevelDestination.entries

    fun navigateToTopLevelDestination(destination: TopLevelDestination) {
        val navOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        when (destination) {
            TopLevelDestination.IMAGES -> {
                navController.navigateToImages(navOptions)
            }
            TopLevelDestination.BUCKETS -> {
                navController.navigateToBuckets(navOptions)
            }
        }
    }
}

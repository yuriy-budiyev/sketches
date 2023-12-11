package com.github.yuriybudiyev.sketches.main.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.github.yuriybudiyev.sketches.core.navigation.buildRoute
import com.github.yuriybudiyev.sketches.core.navigation.destination.NavigationDestination
import com.github.yuriybudiyev.sketches.core.navigation.destination.TopLevelNavigationDestination
import com.github.yuriybudiyev.sketches.core.navigation.navigate
import com.github.yuriybudiyev.sketches.main.navigation.TopLevelNavigationType

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

    val currentNavigationDestination: NavigationDestination?
        @Composable get() {
            val route = navController.currentBackStackEntryAsState().value?.destination?.route
                ?: return null
            return navigationDestinationsInternal[route]
        }

    val currentTopLevelNavigationDestination: TopLevelNavigationDestination?
        @Composable get() {
            val destination = currentNavigationDestination ?: return null
            if (destination is TopLevelNavigationDestination) return destination
            return null
        }

    val currentTopLevelNavigationType: TopLevelNavigationType
        @Composable get() = if (currentTopLevelNavigationDestination != null) {
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                TopLevelNavigationType.BAR
            } else {
                TopLevelNavigationType.RAIL
            }
        } else {
            TopLevelNavigationType.NONE
        }

    val topLevelNavigationDestinations: List<TopLevelNavigationDestination>
        get() = topLevelNavigationDestinationsInternal

    fun navigateToTopLevelDestination(destination: TopLevelNavigationDestination) {
        val options = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        navController.navigate(
            destination,
            options
        )
    }

    fun registerNavigationDestination(destination: NavigationDestination) {
        navigationDestinationsInternal[destination.buildRoute()] = destination
        if (destination is TopLevelNavigationDestination) {
            topLevelNavigationDestinationsInternal += destination
        }
    }

    private val navigationDestinationsInternal: MutableMap<String, NavigationDestination> =
        HashMap()

    private val topLevelNavigationDestinationsInternal: MutableList<TopLevelNavigationDestination> =
        ArrayList()
}

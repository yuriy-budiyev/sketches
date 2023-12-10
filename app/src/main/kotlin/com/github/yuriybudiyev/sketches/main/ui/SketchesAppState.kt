package com.github.yuriybudiyev.sketches.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.window.core.layout.WindowSizeClass
import com.github.yuriybudiyev.sketches.buckets.navigation.BUCKETS_ROUTE
import com.github.yuriybudiyev.sketches.images.navigation.IMAGES_ROUTE
import com.github.yuriybudiyev.sketches.main.navigation.TopLevelDestination

@Stable
class SketchesAppState(
    val navController: NavController,
    val windowSizeClass: WindowSizeClass
) {

    val currentDestination: NavDestination?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination

    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() = when (currentDestination?.route) {
            IMAGES_ROUTE -> TopLevelDestination.IMAGES
            BUCKETS_ROUTE -> TopLevelDestination.BUCKETS
            else -> null
        }
}

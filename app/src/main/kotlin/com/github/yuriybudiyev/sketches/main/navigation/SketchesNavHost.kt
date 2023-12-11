package com.github.yuriybudiyev.sketches.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import com.github.yuriybudiyev.sketches.buckets.navigation.BucketsNavigationDestination
import com.github.yuriybudiyev.sketches.core.navigation.composable
import com.github.yuriybudiyev.sketches.images.navigation.ImagesNavigationDestination
import com.github.yuriybudiyev.sketches.images.ui.ImagesRoute
import com.github.yuriybudiyev.sketches.main.ui.SketchesAppState

@Composable
fun SketchesNavHost(appState: SketchesAppState) {
    NavHost(
        navController = appState.navController,
        startDestination = ImagesNavigationDestination.routeBase
    ) {
        composable(ImagesNavigationDestination) {
            ImagesRoute(onImageClick = {})
        }
        composable(BucketsNavigationDestination) {

        }
    }
}

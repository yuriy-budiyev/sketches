package com.github.yuriybudiyev.sketches.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import com.github.yuriybudiyev.sketches.images.navigation.IMAGES_ROUTE
import com.github.yuriybudiyev.sketches.images.navigation.imagesScreen
import com.github.yuriybudiyev.sketches.main.ui.SketchesAppState

@Composable
fun SketchesNavHost(appState: SketchesAppState) {
    NavHost(
        navController = appState.navController,
        startDestination = IMAGES_ROUTE
    ) {
        imagesScreen { }
    }
}

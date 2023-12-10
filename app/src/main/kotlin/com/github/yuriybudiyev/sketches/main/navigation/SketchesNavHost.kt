package com.github.yuriybudiyev.sketches.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.yuriybudiyev.sketches.images.navigation.IMAGES_ROUTE
import com.github.yuriybudiyev.sketches.images.ui.ImagesRoute

@Composable
fun SketchesNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = IMAGES_ROUTE
    ) {
        composable(route = IMAGES_ROUTE) {
            ImagesRoute(onImageClick = {})
        }
    }
}

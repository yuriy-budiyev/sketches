package com.github.yuriybudiyev.sketches.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.yuriybudiyev.sketches.images.ui.ImagesRoute
import com.github.yuriybudiyev.sketches.images.ui.ImagesScreen

@Composable
fun SketchesNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "images"
    ) {
        composable("images") {
            ImagesRoute(onImageClick = {})
        }
    }
}
package com.github.yuriybudiyev.sketches.main.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.yuriybudiyev.sketches.gallery.ui.GalleryScreen

@Composable
fun SketchesNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "images"
    ) {
        composable("images") {
            GalleryScreen()
        }
    }
}
package com.github.yuriybudiyev.sketches.core.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.yuriybudiyev.sketches.core.navigation.destination.NavigationDestination

fun NavGraphBuilder.composable(
    destination: NavigationDestination,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = destination.route,
        arguments = destination.arguments,
        deepLinks = destination.deepLinks,
        content = content
    )
}

package com.github.yuriybudiyev.sketches.core.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.github.yuriybudiyev.sketches.core.navigation.destination.NavigationDestination

fun NavGraphBuilder.composable(
    destination: NavigationDestination,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = destination.buildRoute(),
        arguments = destination.arguments,
        deepLinks = destination.deepLinks,
        content = content
    )
}

fun NavController.navigate(
    destination: NavigationDestination,
    options: NavOptions? = null,
    vararg args: Any
) {
    navigate(
        destination.buildRouteWithArgs(*args),
        options
    )
}

fun NavigationDestination.buildRoute(): String =
    if (arguments.isEmpty()) {
        routeBase
    } else {
        buildString {
            append(routeBase)
            arguments.forEach { arg ->
                append("/{")
                append(arg.name)
                append("}")
            }
        }
    }

fun NavigationDestination.buildRouteWithArgs(vararg args: Any): String =
    if (args.isEmpty()) {
        routeBase
    } else {
        buildString {
            append(routeBase)
            args.forEach { arg ->
                append("/{")
                append(arg.toString())
                append("}")
            }
        }
    }

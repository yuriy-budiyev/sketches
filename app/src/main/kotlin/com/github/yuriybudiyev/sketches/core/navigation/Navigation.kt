/*
 * MIT License
 *
 * Copyright (c) 2024 Yuriy Budiyev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.yuriybudiyev.sketches.core.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navOptions
import com.github.yuriybudiyev.sketches.core.navigation.destination.NavigationDestination
import com.github.yuriybudiyev.sketches.main.ui.SketchesAppState

fun NavGraphBuilder.composable(
    destination: NavigationDestination,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(
        route = destination.buildRoute(),
        arguments = destination.arguments,
        deepLinks = destination.deepLinks,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        content = content
    )
}

fun NavController.navigate(
    destination: NavigationDestination,
    vararg args: Any,
    optionsBuilder: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(
        destination.buildRouteWithArgs(*args),
        navOptions(optionsBuilder)
    )
}

fun NavigationDestination.registerIn(appState: SketchesAppState): NavigationDestination {
    appState.registerNavigationDestination(this)
    return this
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
                append("/")
                append(arg.toString())
            }
        }
    }

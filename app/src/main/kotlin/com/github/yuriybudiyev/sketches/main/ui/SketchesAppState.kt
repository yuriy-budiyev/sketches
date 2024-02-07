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

package com.github.yuriybudiyev.sketches.main.ui

import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.yuriybudiyev.sketches.core.navigation.buildRoute
import com.github.yuriybudiyev.sketches.core.navigation.destination.NavigationDestination
import com.github.yuriybudiyev.sketches.core.navigation.destination.TopLevelNavigationDestination
import com.github.yuriybudiyev.sketches.core.navigation.navigate

@Composable
fun rememberSketchesAppState(
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): SketchesAppState =
    remember(
        navController,
        coroutineScope
    ) {
        SketchesAppState(
            navController,
            coroutineScope
        )
    }

@Stable
class SketchesAppState(
    val navController: NavHostController,
    val coroutineScope: CoroutineScope,
) {

    val currentNavigationDestination: NavigationDestination?
        @Composable get() {
            val route = navController.currentBackStackEntryAsState().value?.destination?.route
                ?: return null
            return navigationDestinationsInternal[route]
        }

    val topLevelNavigationDestinations: List<TopLevelNavigationDestination>
        get() = topLevelNavigationDestinationsInternal

    fun navigateToTopLevelDestination(destination: TopLevelNavigationDestination) {
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
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

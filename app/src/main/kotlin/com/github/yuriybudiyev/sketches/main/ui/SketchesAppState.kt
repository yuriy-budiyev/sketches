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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.yuriybudiyev.sketches.core.collections.linkedHashMapWithExpectedSize
import com.github.yuriybudiyev.sketches.core.navigation.TopLevelNavigationRoute
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsRoute
import com.github.yuriybudiyev.sketches.feature.images.navigation.ImagesRoute
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer

@Composable
fun rememberSketchesAppState(navController: NavHostController = rememberNavController()): SketchesAppState =
    remember(navController) {
        SketchesAppState(navController)
    }

@Stable
class SketchesAppState(val navController: NavHostController) {

    val currentTopLevelNavigationRoute: TopLevelNavigationRoute?
        @Composable get() = navController.currentBackStackEntryFlow
            .map {
                it.destination.route?.let { route ->
                    topLevelRoutesInternal[route.substringBefore('/')]
                }
            }
            .collectAsState(null).value

    fun navigateToTopLevelNavigationRoute(route: TopLevelNavigationRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    private val topLevelRoutesInternal: MutableMap<String, TopLevelNavigationRoute> =
        linkedHashMapWithExpectedSize<String, TopLevelNavigationRoute>(2).also { routes ->
            routes[serialName<ImagesRoute>()] = ImagesRoute
            routes[serialName<BucketsRoute>()] = BucketsRoute
        }

    val topLevelNavigationRoutes: Collection<TopLevelNavigationRoute> =
        topLevelRoutesInternal.values

    @OptIn(ExperimentalSerializationApi::class)
    private inline fun <reified T: Any> serialName(): String =
        serializer<T>().descriptor.serialName
}

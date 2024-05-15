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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.yuriybudiyev.sketches.core.navigation.RouteInfo
import com.github.yuriybudiyev.sketches.core.navigation.TopLevelRouteInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer

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

class SketchesAppState(
    val navController: NavHostController,
    val coroutineScope: CoroutineScope,
) {

    val currentNavigationRoute: RouteInfo?
        @Composable get() {
            val route = navController.currentBackStackEntryAsState().value?.destination?.route
                ?: return null
            return navigationRoutesInternal[route.substringBefore('/')]
        }

    val topLevelNavigationRoutes: Collection<TopLevelRouteInfo>
        get() = topLevelNavigationRoutesInternal.values

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T: Any> registerNavigationRoute(routeInfo: RouteInfo) {
        registerNavigationRoute(
            serializer<T>().descriptor.serialName,
            routeInfo
        )
    }

    @PublishedApi
    internal fun registerNavigationRoute(
        serialName: String,
        routeInfo: RouteInfo,
    ) {
        navigationRoutesInternal[serialName] = routeInfo
        if (routeInfo is TopLevelRouteInfo) {
            topLevelNavigationRoutesInternal[serialName] = routeInfo
        }
    }

    private val navigationRoutesInternal: MutableMap<String, RouteInfo> = LinkedHashMap()

    private val topLevelNavigationRoutesInternal: MutableMap<String, TopLevelRouteInfo> =
        LinkedHashMap()
}

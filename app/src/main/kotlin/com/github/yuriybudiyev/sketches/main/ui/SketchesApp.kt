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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.collections.LinkedHashMap
import com.github.yuriybudiyev.sketches.core.common.permissions.media.MediaAccess
import com.github.yuriybudiyev.sketches.core.common.permissions.media.checkMediaAccess
import com.github.yuriybudiyev.sketches.core.common.permissions.media.rememberMediaAccessRequestLauncher
import com.github.yuriybudiyev.sketches.core.navigation.TopLevelNavigationRoute
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesOutlinedButton
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsRoute
import com.github.yuriybudiyev.sketches.feature.images.navigation.ImagesRoute
import com.github.yuriybudiyev.sketches.main.navigation.SketchesNavHost
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer

@Composable
fun SketchesApp() {
    val coroutineScope = rememberCoroutineScope()
    val appContextUpdated by rememberUpdatedState(LocalContext.current.applicationContext)
    var mediaAccess by remember { mutableStateOf(appContextUpdated.checkMediaAccess()) }
    val mediaAccessLauncher = rememberMediaAccessRequestLauncher { result ->
        mediaAccess = result
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        mediaAccess = appContextUpdated.checkMediaAccess()
    }
    val colorSchemeUpdated by rememberUpdatedState(MaterialTheme.colorScheme)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colorSchemeUpdated.background,
        contentColor = colorSchemeUpdated.onBackground
    ) {
        when (mediaAccess) {
            MediaAccess.Full, MediaAccess.UserSelected -> {
                val navController = rememberNavController()
                val topLevelNavigationRoutes = rememberTopLevelNavigationRoutes()
                val currentTopLevelRoute by navController.currentTopLevelNavigationRoute(topLevelNavigationRoutes)
                Box(modifier = Modifier.fillMaxSize()) {
                    SketchesNavHost(
                        navController = navController,
                        coroutineScope = coroutineScope,
                        modifier = Modifier.matchParentSize(),
                        onRequestUserSelectedMedia = if (mediaAccess == MediaAccess.UserSelected) {
                            { mediaAccessLauncher.requestMediaAccess() }
                        } else {
                            null
                        }
                    )
                    val view = LocalView.current
                    if (!view.isInEditMode) {
                        SideEffect {
                            val window = (view.context as Activity).window
                            if (currentTopLevelRoute != null) {
                                window.navigationBarColor = Color.Transparent.toArgb()
                            } else {
                                window.navigationBarColor = colorSchemeUpdated.background
                                    .copy(alpha = SketchesColors.UiAlphaLowTransparency)
                                    .toArgb()
                            }
                        }
                    }
                    if (currentTopLevelRoute != null) {
                        val bottomSystemBarHeight = WindowInsets.systemBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                        NavigationBar(
                            containerColor = colorSchemeUpdated.background.copy(alpha = SketchesColors.UiAlphaLowTransparency),
                            contentColor = colorSchemeUpdated.onBackground,
                            modifier = Modifier
                                .height(SketchesDimens.BottomBarHeight + bottomSystemBarHeight)
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                        ) {
                            topLevelNavigationRoutes.forEach { (_, route) ->
                                val selected = route == currentTopLevelRoute
                                NavigationBarItem(
                                    selected = selected,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = colorSchemeUpdated.onPrimary,
                                        unselectedIconColor = colorSchemeUpdated.onBackground,
                                        indicatorColor = colorSchemeUpdated.primary
                                    ),
                                    onClick = {
                                        coroutineScope.launch {
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) {
                                                route.selectedIcon
                                            } else {
                                                route.unselectedIcon
                                            },
                                            contentDescription = stringResource(id = route.titleRes)
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
            MediaAccess.None -> {
                val settingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult(),
                    onResult = {},
                )
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SketchesMessage(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            stringResource(id = R.string.no_images_permission)
                        } else {
                            stringResource(id = R.string.no_storage_permission)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SketchesOutlinedButton(text = stringResource(id = R.string.open_settings)) {
                        coroutineScope.launch {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            intent.data = Uri.fromParts(
                                "package",
                                appContextUpdated.packageName,
                                null
                            )
                            settingsLauncher.launch(intent)
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    coroutineScope.launch {
                        mediaAccessLauncher.requestMediaAccess()
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberTopLevelNavigationRoutes(): Map<String, TopLevelNavigationRoute> =
    remember {
        LinkedHashMap<String, TopLevelNavigationRoute>(expectedSize = 2).also { routes ->
            routes[serialName<ImagesRoute>()] = ImagesRoute
            routes[serialName<BucketsRoute>()] = BucketsRoute
        }
    }

@Composable
private fun NavHostController.currentTopLevelNavigationRoute(
    routes: Map<String, TopLevelNavigationRoute>,
): State<TopLevelNavigationRoute?> =
    currentBackStackEntryFlow
        .map { backStackEntry ->
            backStackEntry.destination.route?.let { route ->
                routes[route.substringBefore('/')]
            }
        }
        .collectAsStateWithLifecycle(null)

@OptIn(ExperimentalSerializationApi::class)
private inline fun <reified T: Any> serialName(): String =
    serializer<T>().descriptor.serialName

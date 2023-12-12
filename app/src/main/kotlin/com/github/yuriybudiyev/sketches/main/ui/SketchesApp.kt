/*
 * MIT License
 *
 * Copyright (c) 2023 Yuriy Budiyev
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

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.navigation.destination.TopLevelNavigationDestination
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.utils.checkPermissionGranted
import com.github.yuriybudiyev.sketches.main.navigation.SketchesNavHost
import com.github.yuriybudiyev.sketches.main.navigation.TopLevelNavigationType

@Composable
fun SketchesApp(
    windowSizeClass: WindowSizeClass,
    appState: SketchesAppState = rememberSketchesAppState(windowSizeClass = windowSizeClass)
) {
    val context = LocalContext.current
    val imagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var permissionGranted by remember {
        mutableStateOf(
            context.checkPermissionGranted(
                imagesPermission
            )
        )
    }
    Surface(color = MaterialTheme.colorScheme.background) {
        if (permissionGranted) {
            MainScreen(appState = appState)
        } else {
            NoPermission()
            val lifecycleOwner by rememberUpdatedState(LocalLifecycleOwner.current)
            DisposableEffect(lifecycleOwner) {
                val lifecycleObserver = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        permissionGranted = context.checkPermissionGranted(imagesPermission)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                }
            }
            val imagesPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                permissionGranted = granted
            }
            LaunchedEffect(Unit) {
                imagesPermissionLauncher.launch(imagesPermission)
            }
        }
    }
}

@Composable
private fun NoPermission() {
    val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        stringResource(id = R.string.no_images_permission)
    } else {
        stringResource(id = R.string.no_storage_permission)
    }
    SketchesCenteredMessage(text = message)
}

@Composable
private fun MainScreen(appState: SketchesAppState) {
    val currentTopLevelNavigationType = appState.currentTopLevelNavigationType
    Scaffold(modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentTopLevelNavigationType == TopLevelNavigationType.BAR) {
                SketchesNavBar(appState)
            }
        }) { contentPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (currentTopLevelNavigationType == TopLevelNavigationType.RAIL) {
                SketchesNavRail(appState)
            }
            SketchesNavHost(appState = appState)
        }
    }
}

@Composable
private fun SketchesNavBar(appState: SketchesAppState) {
    NavigationBar {
        val currentDestination = appState.currentTopLevelNavigationDestination
        appState.topLevelNavigationDestinations.forEach { destination ->
            val selected = destination == currentDestination
            NavigationBarItem(selected = selected,
                onClick = {
                    appState.navigateToTopLevelDestination(destination)
                },
                icon = {
                    TopLevelDestinationIcon(
                        selected,
                        destination
                    )
                },
                label = {
                    TopLevelDestinationLabel(destination)
                })
        }
    }
}

@Composable
private fun SketchesNavRail(appState: SketchesAppState) {
    NavigationRail {
        val currentDestination = appState.currentTopLevelNavigationDestination
        appState.topLevelNavigationDestinations.forEach { destination ->
            val selected = destination == currentDestination
            NavigationRailItem(selected = selected,
                onClick = {
                    appState.navigateToTopLevelDestination(destination)
                },
                icon = {
                    TopLevelDestinationIcon(
                        selected,
                        destination
                    )
                },
                label = {
                    TopLevelDestinationLabel(destination)
                })
        }
    }
}

@Composable
private fun TopLevelDestinationIcon(
    selected: Boolean,
    destination: TopLevelNavigationDestination
) {
    Icon(
        imageVector = if (selected) {
            destination.selectedIcon
        } else {
            destination.unselectedIcon
        },
        contentDescription = null
    )
}

@Composable
private fun TopLevelDestinationLabel(destination: TopLevelNavigationDestination) {
    Text(text = stringResource(id = destination.labelRes))
}

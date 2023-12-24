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

import kotlinx.coroutines.launch
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.navigation.destination.TopLevelNavigationDestination
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesMessage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesOutlinedButton
import com.github.yuriybudiyev.sketches.core.ui.effect.LifecycleEventEffect
import com.github.yuriybudiyev.sketches.core.util.checkAllPermissionsGranted
import com.github.yuriybudiyev.sketches.main.navigation.SketchesNavHost

@Composable
fun SketchesApp(appState: SketchesAppState = rememberSketchesAppState()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    var permissionsGranted by remember {
        mutableStateOf(context.checkAllPermissionsGranted(mediaPermissions))
    }
    Surface(color = MaterialTheme.colorScheme.background) {
        if (permissionsGranted) {
            ContentLayout(appState = appState)
        } else {
            val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                stringResource(id = R.string.no_images_permission)
            } else {
                stringResource(id = R.string.no_storage_permission)
            }
            val mediaPermissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { grantResult ->
                permissionsGranted = checkAllPermissionsGranted(grantResult)
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SketchesMessage(text = message)
                Spacer(modifier = Modifier.height(8.dp))
                SketchesOutlinedButton(text = stringResource(id = R.string.open_settings)) {
                    coroutineScope.launch {

                    }
                }
            }
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                permissionsGranted = context.checkAllPermissionsGranted(mediaPermissions)
            }
            LaunchedEffect(Unit) {
                coroutineScope.launch {
                    mediaPermissionsLauncher.launch(mediaPermissions)
                }
            }
        }
    }
}

@Composable
private fun ContentLayout(appState: SketchesAppState) {
    val currentDestinations = appState.topLevelNavigationDestinations
    val currentDestination = appState.currentNavigationDestination
    Scaffold(modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentDestination is TopLevelNavigationDestination) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    currentDestinations.forEach { destination ->
                        val selected = destination == currentDestination
                        NavigationBarItem(selected = selected,
                            onClick = {
                                appState.navigateToTopLevelDestination(destination)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) {
                                        destination.selectedIcon
                                    } else {
                                        destination.unselectedIcon
                                    },
                                    contentDescription = null
                                )
                            })
                    }
                }
            }
        },
        content = { contentPadding ->
            SketchesNavHost(
                appState = appState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            )
        })
}

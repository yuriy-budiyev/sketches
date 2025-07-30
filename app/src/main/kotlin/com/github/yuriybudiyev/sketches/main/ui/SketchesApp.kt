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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.MediaAccess
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.checkMediaAccess
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.rememberMediaAccessRequestLauncher
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesOutlinedButton
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.main.navigation.SketchesNavHost

@Composable
fun SketchesApp(appState: SketchesAppState = rememberSketchesAppState()) {
    val appContextUpdated by rememberUpdatedState(LocalContext.current.applicationContext)
    var mediaAccess by remember { mutableStateOf(appContextUpdated.checkMediaAccess()) }
    val mediaAccessLauncher = rememberMediaAccessRequestLauncher { result ->
        mediaAccess = result
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        mediaAccess = appContextUpdated.checkMediaAccess()
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        when (mediaAccess) {
            MediaAccess.Full, MediaAccess.UserSelected -> {
                val topLevelNavigationRoutes = appState.topLevelNavigationRoutes
                val currentTopLevelRoute = appState.currentTopLevelNavigationRoute
                Box(modifier = Modifier.fillMaxSize()) {
                    SketchesNavHost(
                        appState = appState,
                        modifier = Modifier.matchParentSize(),
                        onRequestUserSelectedMedia = if (mediaAccess == MediaAccess.UserSelected) {
                            { mediaAccessLauncher.requestMediaAccess() }
                        } else {
                            null
                        }
                    )
                    if (currentTopLevelRoute != null) {
                        val bottomSystemBarHeight = WindowInsets.systemBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.background
                                .copy(alpha = SketchesColors.UiAlphaLowTransparency),
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .height(SketchesDimens.BottomBarHeight + bottomSystemBarHeight)
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                        ) {
                            topLevelNavigationRoutes.forEach { route ->
                                val selected = route == currentTopLevelRoute
                                NavigationBarItem(
                                    selected = selected,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onBackground,
                                        indicatorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    onClick = {
                                        appState.navigateToTopLevelNavigationRoute(route)
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
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts(
                            "package",
                            appContextUpdated.packageName,
                            null
                        )
                        settingsLauncher.launch(intent)
                    }
                }
                LaunchedEffect(Unit) {
                    mediaAccessLauncher.requestMediaAccess()
                }
            }
        }
    }
}

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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.yuriybudiyev.sketches.core.navigation.destination.NavigationDestination
import com.github.yuriybudiyev.sketches.core.navigation.destination.TopLevelNavigationDestination
import com.github.yuriybudiyev.sketches.core.ui.theme.SketchesTheme
import com.github.yuriybudiyev.sketches.main.navigation.SketchesNavHost
import com.github.yuriybudiyev.sketches.main.navigation.TopLevelNavigationType

@Composable
fun SketchesApp(
    windowSizeClass: WindowSizeClass,
    appState: SketchesAppState = rememberSketchesAppState(windowSizeClass = windowSizeClass)
) {
    SketchesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val currentTopLevelNavigationType = appState.currentTopLevelNavigationType
            Scaffold(bottomBar = {
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (currentTopLevelNavigationType == TopLevelNavigationType.NONE) {
                            val currentDestination = appState.currentNavigationDestination
                            if (currentDestination != null) {
                                SketchesTopAppBar(currentDestination)
                            }
                        }
                        SketchesNavHost(appState = appState)
                    }
                }
            }
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
@OptIn(ExperimentalMaterial3Api::class)
private fun SketchesTopAppBar(currentDestination: NavigationDestination) {
    TopAppBar(

        title = {
            Text(
                text = stringResource(
                    id = currentDestination.labelRes
                )
            )
        },
        navigationIcon = {
            Icon(
                imageVector = currentDestination.navigationIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        })
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

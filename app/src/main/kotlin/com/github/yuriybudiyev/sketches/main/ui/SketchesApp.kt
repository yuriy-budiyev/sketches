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
import com.github.yuriybudiyev.sketches.core.navigation.destination.TopLevelNavigationDestination
import com.github.yuriybudiyev.sketches.core.ui.theme.SketchesTheme
import com.github.yuriybudiyev.sketches.main.navigation.SketchesNavHost
import com.github.yuriybudiyev.sketches.main.navigation.TopLevelNavigationType

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SketchesApp(
    windowSizeClass: WindowSizeClass,
    appState: SketchesAppState = rememberSketchesAppState(windowSizeClass = windowSizeClass)
) {
    SketchesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(bottomBar = {
                if (appState.currentTopLevelNavigationType == TopLevelNavigationType.BAR) {
                    SketchesNavBar(appState)
                }
            }) { contentPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {
                    if (appState.currentTopLevelNavigationType == TopLevelNavigationType.RAIL) {
                        SketchesNavRail(appState)
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (appState.currentTopLevelNavigationType == TopLevelNavigationType.NONE) {

                            TopAppBar(title = { })
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

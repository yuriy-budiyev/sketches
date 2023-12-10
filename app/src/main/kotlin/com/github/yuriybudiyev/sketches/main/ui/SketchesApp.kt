package com.github.yuriybudiyev.sketches.main.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.yuriybudiyev.sketches.core.ui.theme.SketchesTheme
import com.github.yuriybudiyev.sketches.main.navigation.SketchesNavHost
import com.github.yuriybudiyev.sketches.main.navigation.TopLevelNavigation


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SketchesApp(
    windowSizeClass: WindowSizeClass,
    appState: SketchesAppState = rememberSketchesAppState(windowSizeClass = windowSizeClass)
) {
    SketchesTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Scaffold(bottomBar = {
                if (appState.currentTopLevelNavigation == TopLevelNavigation.BAR) {
                    NavigationBar {

                    }
                }
            }) { contentPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {                     
                        if (appState.currentTopLevelNavigation == TopLevelNavigation.NONE) {
                            TopAppBar(title = {  })
                        }
                        SketchesNavHost(appState = appState)
                    }
                    if (appState.currentTopLevelNavigation == TopLevelNavigation.RAIL) {
                        NavigationRail {

                        }
                    }
                }
            }
        }
    }
}

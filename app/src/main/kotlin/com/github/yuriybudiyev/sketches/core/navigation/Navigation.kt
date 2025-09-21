package com.github.yuriybudiyev.sketches.core.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("CompositionLocal LocalNavController not present")
}

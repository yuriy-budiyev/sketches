package com.github.yuriybudiyev.sketches.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector

interface TopLevelNavigationDestination: NavigationDestination {

    val selectedIcon: ImageVector

    val unselectedIcon: ImageVector
}

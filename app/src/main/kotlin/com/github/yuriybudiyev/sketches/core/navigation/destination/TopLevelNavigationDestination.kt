package com.github.yuriybudiyev.sketches.core.navigation.destination

import androidx.compose.ui.graphics.vector.ImageVector

interface TopLevelNavigationDestination: NavigationDestination {

    val selectedIcon: ImageVector

    val unselectedIcon: ImageVector
}

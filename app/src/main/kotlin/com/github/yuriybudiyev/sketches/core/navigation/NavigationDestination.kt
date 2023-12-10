package com.github.yuriybudiyev.sketches.core.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink

interface NavigationDestination {

    val route: String

    val arguments: List<NamedNavArgument>

    val deepLinks: List<NavDeepLink>

    @get:StringRes
    val labelRes: Int

    val navigationIcon: ImageVector
}

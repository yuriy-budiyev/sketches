package com.github.yuriybudiyev.sketches.images.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.navigation.destination.TopLevelNavigationDestination
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons

object ImagesNavigationDestination: TopLevelNavigationDestination {

    override val routeBase: String = "images"
    override val arguments: List<NamedNavArgument> = emptyList()
    override val deepLinks: List<NavDeepLink> = emptyList()
    override val labelRes: Int = R.string.main_navigation_images
    override val navigationIcon: ImageVector = SketchesIcons.ImagesNavigation
    override val selectedIcon: ImageVector = SketchesIcons.ImagesSelected
    override val unselectedIcon: ImageVector = SketchesIcons.ImagesUnselected
}

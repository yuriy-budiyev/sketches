package com.github.yuriybudiyev.sketches.buckets.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavOptions
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.navigation.destination.TopLevelNavigationDestination
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons

const val BUCKETS_ROUTE = "buckets"

fun NavController.navigateToBuckets(navOptions: NavOptions?) {
}

object BucketsNavigationDestination: TopLevelNavigationDestination {

    override val routeBase: String = "buckets"
    override val arguments: List<NamedNavArgument> = emptyList()
    override val deepLinks: List<NavDeepLink> = emptyList()
    override val labelRes: Int = R.string.main_navigation_buckets
    override val navigationIcon: ImageVector = SketchesIcons.BucketsNavigation
    override val selectedIcon: ImageVector = SketchesIcons.BucketsSelected
    override val unselectedIcon: ImageVector = SketchesIcons.BucketsUnselected
}

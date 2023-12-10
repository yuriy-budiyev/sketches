package com.github.yuriybudiyev.sketches.images.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.navigation.composable
import com.github.yuriybudiyev.sketches.core.navigation.destination.TopLevelNavigationDestination
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons
import com.github.yuriybudiyev.sketches.images.ui.ImagesRoute

const val IMAGES_ARG_BUCKET_ID = "bucket_id"
const val IMAGES_ROUTE = "images/{$IMAGES_ARG_BUCKET_ID}"

fun NavGraphBuilder.imagesScreen(onImageClick: (Long) -> Unit) {
    composable(ImagesNavigationDestination) {
        ImagesRoute(onImageClick = onImageClick)
    }
}

fun NavController.navigateToImages(navOptions: NavOptions? = null) {
    navigate(
        ImagesNavigationDestination.route,
        navOptions
    )
}

object ImagesNavigationDestination: TopLevelNavigationDestination {

    override val route: String = "images"
    override val arguments: List<NamedNavArgument> = emptyList()
    override val deepLinks: List<NavDeepLink> = emptyList()
    override val labelRes: Int = R.string.main_navigation_images
    override val navigationIcon: ImageVector = SketchesIcons.ImagesNavigation
    override val selectedIcon: ImageVector = SketchesIcons.ImagesSelected
    override val unselectedIcon: ImageVector = SketchesIcons.ImagesUnselected
}

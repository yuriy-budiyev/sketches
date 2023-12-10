package com.github.yuriybudiyev.sketches.images.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.github.yuriybudiyev.sketches.core.navigation.TopLevelNavigationDestination
import com.github.yuriybudiyev.sketches.images.ui.ImagesRoute

const val IMAGES_ARG_BUCKET_ID = "bucket_id"
const val IMAGES_ROUTE = "images/{$IMAGES_ARG_BUCKET_ID}"

fun NavGraphBuilder.imagesScreen(onImageClick: (Long) -> Unit) {
    composable(
        route = IMAGES_ROUTE,
        arguments = listOf(navArgument(IMAGES_ARG_BUCKET_ID) { type = NavType.LongType })
    ) {
        ImagesRoute(onImageClick = onImageClick)
    }
}

fun NavController.navigateToImages(navOptions: NavOptions? = null) {
    navigate(
        IMAGES_ROUTE,
        navOptions
    )
}

object ImagesNavigationDestination: TopLevelNavigationDestination {

    override val route: String
        get() = TODO("Not yet implemented")
    override val arguments: List<NamedNavArgument>
        get() = TODO("Not yet implemented")
    override val deepLinks: List<NavDeepLink>
        get() = TODO("Not yet implemented")
    override val labelRes: Int
        get() = TODO("Not yet implemented")
    override val navigationIcon: ImageVector
        get() = TODO("Not yet implemented")
    override val selectedIcon: ImageVector
        get() = TODO("Not yet implemented")
    override val unselectedIcon: ImageVector
        get() = TODO("Not yet implemented")
}

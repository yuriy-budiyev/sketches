package com.github.yuriybudiyev.sketches.images.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.github.yuriybudiyev.sketches.images.ui.ImagesRoute

const val IMAGES_ARG_BUCKET_ID = "bucket_id"
const val IMAGES_ROUTE = "images/$IMAGES_ARG_BUCKET_ID"

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

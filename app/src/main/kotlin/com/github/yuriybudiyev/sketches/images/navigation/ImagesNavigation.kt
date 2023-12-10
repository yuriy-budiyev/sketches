package com.github.yuriybudiyev.sketches.images.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.github.yuriybudiyev.sketches.images.ui.ImagesRoute

const val BUCKET_ID = "bucket_id"
const val IMAGES_ROUTE = "images/$BUCKET_ID"

fun NavGraphBuilder.imagesScreen(onImageClick: (Long) -> Unit) {
    composable(
        route = IMAGES_ROUTE,
        arguments = listOf(navArgument(BUCKET_ID) { type = NavType.LongType })
    ) {
        ImagesRoute(onImageClick = onImageClick)
    }
}

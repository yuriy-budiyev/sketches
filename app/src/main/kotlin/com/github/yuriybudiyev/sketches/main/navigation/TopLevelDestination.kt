package com.github.yuriybudiyev.sketches.main.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons

enum class TopLevelDestination(
    val icon: ImageVector,
    @StringRes val text: Int
) {

    IMAGES(
        icon = SketchesIcons.Images,
        text = R.string.main_navigation_images
    ),

    BUCKETS(
        icon = SketchesIcons.Buckets,
        text = R.string.main_navigation_buckets
    )
}

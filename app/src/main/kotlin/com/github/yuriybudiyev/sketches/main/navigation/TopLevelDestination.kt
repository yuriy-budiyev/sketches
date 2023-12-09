package com.github.yuriybudiyev.sketches.main.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons

enum class TopLevelDestination(
    val icon: ImageVector,
    @StringRes val label: Int
) {

    IMAGES(
        icon = SketchesIcons.Images,
        label = R.string.main_navigation_images
    ),

    BUCKETS(
        icon = SketchesIcons.Buckets,
        label = R.string.main_navigation_buckets
    )
}

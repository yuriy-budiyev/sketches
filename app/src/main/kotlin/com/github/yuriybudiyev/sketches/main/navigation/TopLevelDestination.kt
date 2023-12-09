package com.github.yuriybudiyev.sketches.main.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons

enum class TopLevelDestination(
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector,
    @StringRes val labelRes: Int
) {

    IMAGES(
        iconSelected = SketchesIcons.ImagesSelected,
        iconUnselected = SketchesIcons.ImagesUnselected,
        labelRes = R.string.main_navigation_images
    ),

    BUCKETS(
        iconSelected = SketchesIcons.BucketsSelected,
        iconUnselected = SketchesIcons.BucketsUnselected,
        labelRes = R.string.main_navigation_buckets
    )
}

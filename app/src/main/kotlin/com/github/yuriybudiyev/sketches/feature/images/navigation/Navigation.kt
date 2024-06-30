/*
 * MIT License
 *
 * Copyright (c) 2024 Yuriy Budiyev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.yuriybudiyev.sketches.feature.images.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.navigation.TopLevelNavigationRoute
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import com.github.yuriybudiyev.sketches.feature.images.ui.ImagesRoute
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

fun NavGraphBuilder.imagesScreen(
    onImageClick: (index: Int, file: MediaStoreFile) -> Unit,
    onRequestUserSelectedMedia: (() -> Unit)? = null,
) {
    composable<ImagesRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) {
        ImagesRoute(
            onImageClick = onImageClick,
            onRequestUserSelectedMedia = onRequestUserSelectedMedia,
        )
    }
}

@Serializable
data object ImagesRoute: TopLevelNavigationRoute {

    @Transient
    @get:StringRes
    override val titleRes: Int = R.string.images

    @Transient
    override val selectedIcon: ImageVector = SketchesIcons.ImagesSelected

    @Transient
    override val unselectedIcon: ImageVector = SketchesIcons.ImagesUnselected
}

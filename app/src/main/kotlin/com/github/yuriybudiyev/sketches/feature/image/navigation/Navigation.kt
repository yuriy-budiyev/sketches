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

package com.github.yuriybudiyev.sketches.feature.image.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.github.yuriybudiyev.sketches.core.navigation.NavigationRoute
import kotlinx.serialization.Serializable

fun NavController.navigateToImageScreen(
    imageIndex: Int,
    imageId: Long,
    bucketId: Long? = null,
    navOptions: NavOptions? = null,
) {
    navigate(
        ImageRoute(
            imageIndex,
            imageId,
            bucketId
        ),
        navOptions
    )
}

fun NavGraphBuilder.registerImageScreen() {
    composable<ImageRoute>(
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
    ) { backStackEntry ->
        com.github.yuriybudiyev.sketches.feature.image.ui.ImageRoute()
    }
}

@Serializable
data class ImageRoute(
    val imageIndex: Int,
    val imageId: Long,
    val bucketId: Long?,
): NavigationRoute

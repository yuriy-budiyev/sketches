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

package com.github.yuriybudiyev.sketches.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.github.yuriybudiyev.sketches.core.navigation.LocalNavController
import com.github.yuriybudiyev.sketches.feature.bucket.navigation.navigateToBucketScreen
import com.github.yuriybudiyev.sketches.feature.bucket.navigation.registerBucketScreen
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsRoute
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.registerBucketsScreen
import com.github.yuriybudiyev.sketches.feature.image.navigation.navigateToImageScreen
import com.github.yuriybudiyev.sketches.feature.image.navigation.registerImageScreen
import com.github.yuriybudiyev.sketches.feature.images.navigation.ImagesRoute
import com.github.yuriybudiyev.sketches.feature.images.navigation.registerImagesScreen
import com.github.yuriybudiyev.sketches.main.ui.SketchesAppState

@Composable
fun SketchesNavHost(
    appState: SketchesAppState,
    modifier: Modifier = Modifier,
    onRequestUserSelectedMedia: (() -> Unit)? = null,
) {
    val navController = appState.navController
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(
            modifier = modifier,
            navController = navController,
            startDestination = ImagesRoute,
        ) {
            appState.registerTopLevelNavigationRoute(ImagesRoute)
            registerImagesScreen(
                onImageClick = { index, image ->
                    navController.navigateToImageScreen(
                        imageIndex = index,
                        imageId = image.id
                    )
                },
                onRequestUserSelectedMedia = onRequestUserSelectedMedia,
            )
            appState.registerTopLevelNavigationRoute(BucketsRoute)
            registerBucketsScreen(
                onBucketClick = { _, bucket ->
                    navController.navigateToBucketScreen(
                        bucketId = bucket.id,
                        bucketName = bucket.name,
                    )
                },
            )
            registerImageScreen()
            registerBucketScreen(
                onImageClick = { index, image ->
                    navController.navigateToImageScreen(
                        imageIndex = index,
                        imageId = image.id,
                        bucketId = image.bucketId,
                    )
                },
            )
        }
    }
}

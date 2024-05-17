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

import android.content.Intent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.feature.bucket.navigation.BucketRoute
import com.github.yuriybudiyev.sketches.feature.bucket.ui.BucketRoute
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsRoute
import com.github.yuriybudiyev.sketches.feature.buckets.ui.BucketsRoute
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageRoute
import com.github.yuriybudiyev.sketches.feature.image.ui.ImageRoute
import com.github.yuriybudiyev.sketches.feature.images.navigation.ImagesRoute
import com.github.yuriybudiyev.sketches.feature.images.ui.ImagesRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SketchesNavHost(
    navController: NavHostController,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
    onRequestUserSelectedMedia: (() -> Unit)? = null,
) {
    val onRequestUserSelectedMediaUpdated by rememberUpdatedState(onRequestUserSelectedMedia)
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = ImagesRoute
    ) {
        composable<ImagesRoute>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            ImagesRoute(
                onImageClick = { index, image ->
                    coroutineScope.launch {
                        navController.navigate(
                            ImageRoute(
                                imageIndex = index,
                                imageId = image.id,
                                bucketId = Long.MIN_VALUE
                            )
                        )
                    }
                },
                onRequestUserSelectedMedia = onRequestUserSelectedMediaUpdated,
            )
        }
        composable<BucketsRoute>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            BucketsRoute(
                onBucketClick = { _, bucket ->
                    coroutineScope.launch {
                        navController.navigate(
                            BucketRoute(
                                bucketId = bucket.id,
                                bucketName = bucket.name
                            )
                        )
                    }
                },
            )
        }
        composable<BucketRoute>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<BucketRoute>()
            BucketRoute(
                id = route.bucketId,
                name = route.bucketName,
                onImageClick = { index, image ->
                    coroutineScope.launch {
                        navController.navigate(
                            ImageRoute(
                                imageIndex = index,
                                imageId = image.id,
                                bucketId = image.bucketId
                            )
                        )
                    }
                },
            )
        }
        composable<ImageRoute>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<ImageRoute>()
            val context = LocalContext.current
            val shareTitle = stringResource(id = R.string.share_image)
            ImageRoute(
                fileIndex = route.imageIndex,
                fileId = route.imageId,
                bucketId = route.bucketId,
                onShare = { _, file ->
                    coroutineScope.launch {
                        context.startActivity(
                            Intent
                                .createChooser(
                                    Intent(Intent.ACTION_SEND)
                                        .putExtra(
                                            Intent.EXTRA_STREAM,
                                            file.uri
                                        )
                                        .setType(file.mimeType),
                                    shareTitle
                                )
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
            )
        }
    }
}

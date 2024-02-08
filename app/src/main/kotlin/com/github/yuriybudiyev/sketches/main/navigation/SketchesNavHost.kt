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
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.navigation.buildRoute
import com.github.yuriybudiyev.sketches.core.navigation.composable
import com.github.yuriybudiyev.sketches.core.navigation.navigate
import com.github.yuriybudiyev.sketches.core.navigation.registerIn
import com.github.yuriybudiyev.sketches.feature.bucket.navigation.BucketNavigationDestination
import com.github.yuriybudiyev.sketches.feature.bucket.ui.BucketRoute
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsNavigationDestination
import com.github.yuriybudiyev.sketches.feature.buckets.ui.BucketsRoute
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageNavigationDestination
import com.github.yuriybudiyev.sketches.feature.image.ui.ImageRoute
import com.github.yuriybudiyev.sketches.feature.images.navigation.ImagesNavigationDestination
import com.github.yuriybudiyev.sketches.feature.images.ui.ImagesRoute
import com.github.yuriybudiyev.sketches.main.ui.SketchesAppState

@Composable
fun SketchesNavHost(
    appState: SketchesAppState,
    modifier: Modifier = Modifier,
) {
    NavHost(
        modifier = modifier,
        navController = appState.navController,
        startDestination = ImagesNavigationDestination.buildRoute()
    ) {
        composable(ImagesNavigationDestination.registerIn(appState)) {
            ImagesRoute(
                onImageClick = { index, image ->
                    appState.coroutineScope.launch {
                        appState.navController.navigate(
                            ImageNavigationDestination,
                            index,
                            image.id,
                            Long.MIN_VALUE
                        )
                    }
                },
            )
        }
        composable(BucketsNavigationDestination.registerIn(appState)) {
            BucketsRoute(
                onBucketClick = { _, bucket ->
                    appState.coroutineScope.launch {
                        appState.navController.navigate(
                            BucketNavigationDestination,
                            bucket.id,
                            bucket.name
                        )
                    }
                },
            )
        }
        composable(BucketNavigationDestination.registerIn(appState)) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getLong(
                BucketNavigationDestination.Arguments.BUCKET_ID,
                Long.MIN_VALUE
            ) ?: Long.MIN_VALUE
            val bucketName =
                backStackEntry.arguments?.getString(BucketNavigationDestination.Arguments.BUCKET_NAME)
            BucketRoute(
                id = bucketId,
                name = bucketName,
                onImageClick = { index, image ->
                    appState.coroutineScope.launch {
                        appState.navController.navigate(
                            ImageNavigationDestination,
                            index,
                            image.id,
                            image.bucketId
                        )
                    }
                },
            )
        }
        composable(ImageNavigationDestination.registerIn(appState)) { backStackEntry ->
            val imageIndex = backStackEntry.arguments?.getInt(
                ImageNavigationDestination.Arguments.IMAGE_INDEX,
                -1
            ) ?: -1
            val imageId = backStackEntry.arguments?.getLong(
                ImageNavigationDestination.Arguments.IMAGE_ID,
                Long.MIN_VALUE
            ) ?: Long.MIN_VALUE
            val bucketId = backStackEntry.arguments?.getLong(
                ImageNavigationDestination.Arguments.BUCKET_ID,
                Long.MIN_VALUE
            ) ?: Long.MIN_VALUE
            val context = LocalContext.current
            val shareTitle = stringResource(id = R.string.share_image)
            ImageRoute(
                fileIndex = imageIndex,
                fileId = imageId,
                bucketId = bucketId,
                onShare = { _, file ->
                    appState.coroutineScope.launch {
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

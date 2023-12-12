/*
 * MIT License
 *
 * Copyright (c) 2023 Yuriy Budiyev
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
import androidx.navigation.compose.NavHost
import com.github.yuriybudiyev.sketches.bucket.navigation.BucketNavigationDestination
import com.github.yuriybudiyev.sketches.bucket.ui.BucketRoute
import com.github.yuriybudiyev.sketches.buckets.navigation.BucketsNavigationDestination
import com.github.yuriybudiyev.sketches.buckets.ui.BucketsRoute
import com.github.yuriybudiyev.sketches.core.navigation.buildRoute
import com.github.yuriybudiyev.sketches.core.navigation.composable
import com.github.yuriybudiyev.sketches.core.navigation.navigate
import com.github.yuriybudiyev.sketches.core.navigation.registerIn
import com.github.yuriybudiyev.sketches.images.navigation.ImagesNavigationDestination
import com.github.yuriybudiyev.sketches.images.ui.ImagesRoute
import com.github.yuriybudiyev.sketches.main.ui.SketchesAppState

@Composable
fun SketchesNavHost(appState: SketchesAppState) {
    NavHost(
        navController = appState.navController,
        startDestination = ImagesNavigationDestination.buildRoute()
    ) {
        composable(ImagesNavigationDestination.registerIn(appState)) {
            ImagesRoute(onImageClick = { index, image -> })
        }
        composable(BucketsNavigationDestination.registerIn(appState)) {
            BucketsRoute(onBucketClick = { _, bucket ->
                appState.navController.navigate(
                    BucketNavigationDestination,
                    bucket.id,
                    bucket.name
                )
            })
        }
        composable(BucketNavigationDestination.registerIn(appState)) { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getLong(
                BucketNavigationDestination.Args.BUCKET_ID,
                -1L
            ) ?: -1L
            val bucketName =
                backStackEntry.arguments?.getString(BucketNavigationDestination.Args.BUCKET_NAME)
            BucketRoute(id = bucketId,
                name = bucketName,
                onImageClick = { index, image ->

                })
        }
    }
}

/*
 * MIT License
 *
 * Copyright (c) 2025 Yuriy Budiyev
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

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.github.yuriybudiyev.sketches.feature.bucket.navigation.BucketNavRoute
import com.github.yuriybudiyev.sketches.feature.bucket.ui.BucketRoute
import com.github.yuriybudiyev.sketches.feature.bucket.ui.BucketScreenViewModel
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsNavRoute
import com.github.yuriybudiyev.sketches.feature.buckets.ui.BucketsRoute
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageNavRoute
import com.github.yuriybudiyev.sketches.feature.image.ui.ImageRoute
import com.github.yuriybudiyev.sketches.feature.image.ui.ImageScreenViewModel
import com.github.yuriybudiyev.sketches.feature.images.navigation.ImagesNavRoute
import com.github.yuriybudiyev.sketches.feature.images.ui.ImagesRoute

@Composable
@NonRestartableComposable
fun SketchesNavDisplay(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { backStack.removeLastOrNull() },
    onRequestUserSelectedMedia: (() -> Unit)? = null,
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = onBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = {
            ContentTransform(
                fadeIn(),
                fadeOut(),
                sizeTransform = null,
            )
        },
        popTransitionSpec = {
            ContentTransform(
                fadeIn(),
                fadeOut(),
                sizeTransform = null,
            )
        },
        predictivePopTransitionSpec = {
            ContentTransform(
                fadeIn(),
                fadeOut(),
                sizeTransform = null,
            )
        },
        entryProvider = entryProvider {
            entry<ImagesNavRoute> {
                ImagesRoute(
                    viewModel = hiltViewModel(),
                    onImageClick = { index, file ->
                        backStack.add(
                            ImageNavRoute(
                                imageIndex = index,
                                imageId = file.id,
                                bucketId = null
                            )
                        )
                    },
                    onRequestUserSelectedMedia = onRequestUserSelectedMedia,
                )
            }
            entry<BucketsNavRoute> {
                BucketsRoute(
                    viewModel = hiltViewModel(),
                    onBucketClick = { index, bucket ->
                        backStack.add(
                            BucketNavRoute(
                                bucketId = bucket.id,
                                bucketName = bucket.name,
                            )
                        )
                    }
                )
            }
            entry<BucketNavRoute> { route ->
                BucketRoute(
                    viewModel = hiltViewModel<BucketScreenViewModel, BucketScreenViewModel.Factory>(
                        creationCallback = { factory -> factory.create(route) }
                    ),
                    onImageClick = { index, file ->
                        backStack.add(
                            ImageNavRoute(
                                imageIndex = index,
                                imageId = file.id,
                                bucketId = file.bucketId
                            )
                        )
                    }
                )
            }
            entry<ImageNavRoute> { route ->
                ImageRoute(
                    viewModel = hiltViewModel<ImageScreenViewModel, ImageScreenViewModel.Factory>(
                        creationCallback = { factory -> factory.create(route) }
                    ),
                )
            }
        }
    )
}

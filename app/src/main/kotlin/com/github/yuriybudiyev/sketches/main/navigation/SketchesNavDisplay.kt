package com.github.yuriybudiyev.sketches.main.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import com.github.yuriybudiyev.sketches.core.navigation.NavRoute
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
    backStack: NavBackStack<NavRoute>,
    modifier: Modifier = Modifier,
    onRequestUserSelectedMedia: (() -> Unit)? = null,
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
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

@Composable
fun rememberSketchesNavBackStack(root: NavRoute): NavBackStack<NavRoute> =
    rememberSerializable(
        serializer = NavBackStackSerializer(NavKeySerializer()),
    ) {
        NavBackStack(root)
    }

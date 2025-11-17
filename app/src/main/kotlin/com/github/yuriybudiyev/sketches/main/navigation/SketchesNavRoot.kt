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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import com.github.yuriybudiyev.sketches.core.navigation.LocalNavResultStore
import com.github.yuriybudiyev.sketches.core.navigation.NavRoute
import com.github.yuriybudiyev.sketches.core.navigation.TopLevelNavRoute
import com.github.yuriybudiyev.sketches.core.navigation.rememberNavResultStore
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
fun SketchesNavRoot(
    modifier: Modifier = Modifier,
    onRequestUserSelectedMedia: (() -> Unit)? = null,
) {
    val rootRoutes = remember {
        listOf(
            ImagesNavRoute,
            BucketsNavRoute,
        )
    }
    val navBackStack = remember {
        SnapshotStateList<NavRoute>().apply {
            add(rootRoutes.first())
        }
    }
    val saveableStateHolder = rememberSaveableStateHolder()
    val saveableStateNavEntryDecorator = remember {
        NavEntryDecorator<NavRoute>(
            onPop = { contentKey ->
                if (contentKey !is TopLevelNavRoute) {
                    saveableStateHolder.removeState(contentKey)
                }
            },
            decorate = { navEntry ->
                saveableStateHolder.SaveableStateProvider(navEntry.contentKey) {
                    navEntry.Content()
                }
            },
        )
    }
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
    val viewModelStoreNavEntryDecorator = remember(viewModelStoreOwner) {
        val navEntryViewModelProvider = ViewModelProvider.create(
            store = viewModelStoreOwner.viewModelStore,
            factory = viewModelFactory { initializer { NavEntryViewModel() } },
        )
        val navEntryViewModel = navEntryViewModelProvider[NavEntryViewModel::class]
        return@remember NavEntryDecorator<NavRoute>(
            onPop = ({ contentKey ->
                if (contentKey !is TopLevelNavRoute) {
                    navEntryViewModel.clearViewModelStore(contentKey)
                }
            }),
            decorate = { navEntry ->
                val navEntryViewModelStore =
                    navEntryViewModel.getOrCreateViewModelStore(navEntry.contentKey)
                val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
                val childViewModelStoreOwner = remember {
                    object:
                        ViewModelStoreOwner,
                        SavedStateRegistryOwner by savedStateRegistryOwner,
                        HasDefaultViewModelProviderFactory {

                        override val viewModelStore: ViewModelStore
                            get() = navEntryViewModelStore

                        override val defaultViewModelProviderFactory: ViewModelProvider.Factory
                            get() = SavedStateViewModelFactory()

                        override val defaultViewModelCreationExtras: CreationExtras
                            get() = MutableCreationExtras().also { extras ->
                                extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
                                extras[VIEW_MODEL_STORE_OWNER_KEY] = this
                            }

                        init {
                            require(lifecycle.currentState == Lifecycle.State.INITIALIZED)
                            enableSavedStateHandles()
                        }
                    }
                }
                CompositionLocalProvider(LocalViewModelStoreOwner provides childViewModelStoreOwner) {
                    navEntry.Content()
                }
            },
        )
    }
    val navEntries = rememberDecoratedNavEntries(
        backStack = navBackStack,
        entryDecorators = listOf(
            saveableStateNavEntryDecorator,
            viewModelStoreNavEntryDecorator,
        ),
        entryProvider = entryProvider {
            entry<ImagesNavRoute> {
                ImagesRoute(
                    viewModel = hiltViewModel(),
                    onImageClick = { index, file ->
                        navBackStack.add(
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
                    onBucketClick = { _, bucket ->
                        navBackStack.add(
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
                        navBackStack.add(
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
        },
    )
    val sceneState = rememberSceneState(
        entries = navEntries,
        sceneStrategy = SinglePaneSceneStrategy(),
        onBack = { }, //TODO
    )
    val scene = sceneState.currentScene
    val currentInfo = SceneInfo(scene)
    val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }
    val gestureState = rememberNavigationEventState(
        currentInfo = currentInfo,
        backInfo = previousSceneInfos,
    )
    NavigationBackHandler(
        state = gestureState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCompleted = {
            repeat(navEntries.size - scene.previousEntries.size) { } //TODO
        },
    )
    val navResultStore = rememberNavResultStore()
    CompositionLocalProvider(LocalNavResultStore.provides(navResultStore)) {
        NavDisplay(
            sceneState,
            gestureState,
            modifier,
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
        )
    }
}

private class NavEntryViewModel: ViewModel() {

    fun getOrCreateViewModelStore(contentKey: Any): ViewModelStore =
        viewModelStores.getOrPut(contentKey) { ViewModelStore() }

    fun clearViewModelStore(contentKey: Any) {
        viewModelStores.remove(contentKey)?.clear()
    }

    override fun onCleared() {
        for ((_, viewModelStore) in viewModelStores) {
            viewModelStore.clear()
        }
    }

    private val viewModelStores: LinkedHashMap<Any, ViewModelStore> = LinkedHashMap()
}

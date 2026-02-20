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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.HasDefaultViewModelProviderFactory
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
import com.github.yuriybudiyev.sketches.core.navigation.LocalNavSharedTransitionScope
import com.github.yuriybudiyev.sketches.core.navigation.LocalRootNavBarController
import com.github.yuriybudiyev.sketches.core.navigation.NavRoute
import com.github.yuriybudiyev.sketches.core.navigation.RootNavBarController
import com.github.yuriybudiyev.sketches.core.navigation.RootNavRoute
import com.github.yuriybudiyev.sketches.core.navigation.rememberNavResultStore
import com.github.yuriybudiyev.sketches.core.platform.bars.LocalSystemBarsController
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.OnRequestMediaAccess
import com.github.yuriybudiyev.sketches.core.saver.SnapshotStateListSaver
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.feature.bucket.navigation.BucketNavRoute
import com.github.yuriybudiyev.sketches.feature.bucket.navigation.registerBucketNavRoute
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsNavRoute
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.registerBucketsNavRoute
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageNavRoute
import com.github.yuriybudiyev.sketches.feature.image.navigation.registerImageNavRoute
import com.github.yuriybudiyev.sketches.feature.images.navigation.ImagesNavRoute
import com.github.yuriybudiyev.sketches.feature.images.navigation.registerImagesNavRoute

@Composable
fun SketchesNavRoot(
    modifier: Modifier = Modifier,
    onRequestMediaAccess: OnRequestMediaAccess,
) {
    val rootRoutes = remember {
        listOf(
            ImagesNavRoute,
            BucketsNavRoute,
        )
    }
    val initialRoute = remember { rootRoutes.first() }
    val navBackStack =
        rememberSaveable(saver = SnapshotStateListSaver()) {
            SnapshotStateList<NavRoute>().apply {
                add(initialRoute)
            }
        }
    val currentRouteIsRoot by remember {
        derivedStateOf {
            navBackStack.lastOrNull() is RootNavRoute
        }
    }
    val pushNavBackStack = remember { fun(route: NavRoute) { navBackStack.add(route) } }
    val popNavBackStack = remember { fun() { navBackStack.removeLastOrNull() } }
    val navEntryProvider = remember {
        entryProvider {
            registerImagesNavRoute(
                onImageClick = { index, file ->
                    pushNavBackStack(
                        ImageNavRoute(
                            imageIndex = index,
                            imageId = file.id,
                            bucketId = null,
                        ),
                    )
                },
                onRequestMediaAccess = onRequestMediaAccess,
            )
            registerBucketsNavRoute(
                onBucketClick = { _, bucket ->
                    pushNavBackStack(
                        BucketNavRoute(
                            bucketId = bucket.id,
                            bucketName = bucket.name,
                        ),
                    )
                },
            )
            registerBucketNavRoute(
                onImageClick = { index, file ->
                    pushNavBackStack(
                        ImageNavRoute(
                            imageIndex = index,
                            imageId = file.id,
                            bucketId = file.bucketId,
                        ),
                    )
                },
            )
            registerImageNavRoute()
        }
    }
    val saveableStateHolder = rememberSaveableStateHolder()
    val viewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        }
    val navEntryDecorator = remember(viewModelStoreOwner) {
        val viewModelStoreViewModelProvider = ViewModelProvider.create(
            store = viewModelStoreOwner.viewModelStore,
            factory = viewModelFactory { initializer { ViewModelStoreViewModel() } },
        )
        val viewModelStoreViewModel =
            viewModelStoreViewModelProvider[ViewModelStoreViewModel::class]
        return@remember NavEntryDecorator<NavRoute>(
            onPop = ({ contentKey ->
                if (contentKey !is RootNavRoute) {
                    viewModelStoreViewModel.clearViewModelStore(contentKey)
                    saveableStateHolder.removeState(contentKey)
                }
            }),
            decorate = { navEntry ->
                saveableStateHolder.SaveableStateProvider(navEntry.contentKey) {
                    val navEntryViewModelStore =
                        viewModelStoreViewModel.getOrCreateViewModelStore(navEntry.contentKey)
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
                                enableSavedStateHandles()
                            }
                        }
                    }
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner.provides(childViewModelStoreOwner),
                    ) {
                        navEntry.Content()
                    }
                }
            },
        )
    }
    val navEntries = rememberDecoratedNavEntries(
        backStack = navBackStack,
        entryDecorators = listOf(navEntryDecorator),
        entryProvider = navEntryProvider,
    )
    val sceneState = rememberSceneState(
        entries = navEntries,
        sceneStrategy = SinglePaneSceneStrategy(),
        onBack = popNavBackStack,
    )
    val currentScene = sceneState.currentScene
    val currentInfo = SceneInfo(currentScene)
    val previousSceneInfos = sceneState.previousScenes.map { scene -> SceneInfo(scene) }
    val navEventState = rememberNavigationEventState(
        currentInfo = currentInfo,
        backInfo = previousSceneInfos,
    )
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = currentScene.previousEntries.isNotEmpty(),
        onBackCompleted = {
            repeat(navEntries.size - currentScene.previousEntries.size) { popNavBackStack() }
        },
    )
    val navResultStore = rememberNavResultStore()
    val rootNavBarController = rememberRootNavBarController()
    Box(modifier = modifier) {
        SharedTransitionScope { transitionModifier ->
            CompositionLocalProvider(
                LocalNavResultStore.provides(navResultStore),
                LocalRootNavBarController.provides(rootNavBarController),
                LocalNavSharedTransitionScope.provides(this@SharedTransitionScope),
            ) {
                NavDisplay(
                    sceneState = sceneState,
                    navigationEventState = navEventState,
                    modifier = Modifier
                        .matchParentSize()
                        .then(transitionModifier),
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
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
        ) {
            AnimatedVisibility(
                visible = currentRouteIsRoot && rootNavBarController.isRootNavBarVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SketchesDimens.Material3AppBarHeight),
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.background
                                .copy(alpha = SketchesColors.UiAlphaLowTransparency),
                            RectangleShape,
                        )
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    val topRootRoute = navBackStack.findClosestRoot()
                    for (route in rootRoutes) {
                        val selected = route == topRootRoute
                        NavigationBarItem(
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                unselectedIconColor = MaterialTheme.colorScheme.onBackground,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                            ),
                            onClick = {
                                if (route == topRootRoute) {
                                    rootNavBarController.dispatchOnClick(route)
                                } else {
                                    if (route == initialRoute) {
                                        navBackStack.clear()
                                    }
                                    pushNavBackStack(route)
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(
                                        if (selected) {
                                            route.selectedIconRes
                                        } else {
                                            route.unselectedIconRes
                                        },
                                    ),
                                    contentDescription = stringResource(route.titleRes),
                                )
                            },
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = LocalSystemBarsController.current.isSystemBarsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding(),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.background
                                .copy(alpha = SketchesColors.UiAlphaLowTransparency),
                            RectangleShape,
                        )
                        .fillMaxSize(),
                )
            }
        }
    }
}

private fun List<NavRoute>.findClosestRoot(): RootNavRoute? {
    val iterator = listIterator(size)
    while (iterator.hasPrevious()) {
        val route = iterator.previous()
        if (route is RootNavRoute) {
            return route
        }
    }
    return null
}

private class ViewModelStoreViewModel: ViewModel() {

    fun getOrCreateViewModelStore(key: Any): ViewModelStore =
        viewModelStores.getOrPut(key) { ViewModelStore() }

    fun clearViewModelStore(key: Any) {
        viewModelStores.remove(key)?.clear()
    }

    override fun onCleared() {
        for ((_, viewModelStore) in viewModelStores) {
            viewModelStore.clear()
        }
    }

    private val viewModelStores: LinkedHashMap<Any, ViewModelStore> = LinkedHashMap()
}

@Composable
private fun rememberRootNavBarController(): RootNavBarControllerImpl =
    rememberSaveable(saver = RootNavBarControllerImplSaver()) { RootNavBarControllerImpl() }

private class RootNavBarControllerImpl: RootNavBarController {

    override var isRootNavBarVisible: Boolean by mutableStateOf(true)

    override fun showRootNavBar() {
        isRootNavBarVisible = true
    }

    override fun hideRootNavBar() {
        isRootNavBarVisible = false
    }

    override fun setOnClickListener(
        route: RootNavRoute,
        onClick: () -> Unit,
    ) {
        listeners[route] = onClick
    }

    override fun clearOnClickListener(route: RootNavRoute) {
        listeners.remove(route)
    }

    fun dispatchOnClick(route: RootNavRoute) {
        listeners[route]?.invoke()
    }

    private val listeners: HashMap<RootNavRoute, () -> Unit> = HashMap()
}

private class RootNavBarControllerImplSaver: Saver<RootNavBarControllerImpl, Boolean> {

    override fun SaverScope.save(value: RootNavBarControllerImpl): Boolean =
        value.isRootNavBarVisible

    override fun restore(value: Boolean): RootNavBarControllerImpl {
        val controller = RootNavBarControllerImpl()
        controller.isRootNavBarVisible = value
        return controller
    }
}

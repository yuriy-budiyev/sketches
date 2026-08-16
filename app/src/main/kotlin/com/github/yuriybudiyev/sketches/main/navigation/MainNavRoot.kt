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

import android.os.Parcelable
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.github.yuriybudiyev.sketches.core.saveable.rememberSaveableSnapshotStateList
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultAnimationSpec
import com.github.yuriybudiyev.sketches.core.ui.colors.withLowTransparency
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens
import com.github.yuriybudiyev.sketches.feature.bookmarks.navigation.BookmarksNavRoute
import com.github.yuriybudiyev.sketches.feature.bookmarks.navigation.registerBookmarksNavRoute
import com.github.yuriybudiyev.sketches.feature.bucket.navigation.BucketNavRoute
import com.github.yuriybudiyev.sketches.feature.bucket.navigation.registerBucketNavRoute
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsNavRoute
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.registerBucketsNavRoute
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageNavRoute
import com.github.yuriybudiyev.sketches.feature.image.navigation.registerImageNavRoute
import com.github.yuriybudiyev.sketches.feature.images.navigation.ImagesNavRoute
import com.github.yuriybudiyev.sketches.feature.images.navigation.registerImagesNavRoute
import kotlinx.parcelize.Parcelize

@Composable
fun MainNavRoot(
    modifier: Modifier = Modifier,
    onRequestMediaAccess: OnRequestMediaAccess,
) {
    val rootRoutes = remember {
        listOf(
            ImagesNavRoute,
            BucketsNavRoute,
            BookmarksNavRoute,
        )
    }
    val initialRoute = remember { rootRoutes.first() }
    val navBackStack = rememberSaveableSnapshotStateList<NavRoute> {
        add(initialRoute)
    }
    val topRootRoute by remember {
        derivedStateOf(structuralEqualityPolicy()) {
            navBackStack.lastOrNull { route -> route is RootNavRoute } as? RootNavRoute
        }
    }
    val currentRouteIsRoot by remember {
        derivedStateOf(structuralEqualityPolicy()) {
            navBackStack.lastOrNull() is RootNavRoute
        }
    }
    val navEntryProvider = remember {
        entryProvider {
            registerImagesNavRoute(
                onImageClick = { index, file ->
                    navBackStack.add(
                        ImageNavRoute(
                            data = ImageNavRoute.Data.Images(
                                imageIndex = index,
                                imageId = file.id,
                            ),
                        ),
                    )
                },
                onRequestMediaAccess = onRequestMediaAccess,
            )
            registerBucketsNavRoute(
                onBucketClick = { _, bucket ->
                    navBackStack.add(
                        BucketNavRoute(
                            bucketId = bucket.id,
                            bucketName = bucket.name,
                        ),
                    )
                },
            )
            registerBookmarksNavRoute(
                onImageClick = { index, file ->
                    navBackStack.add(
                        ImageNavRoute(
                            data = ImageNavRoute.Data.Bookmarks(
                                imageIndex = index,
                                imageId = file.id,
                            ),
                        ),
                    )
                },
            )
            registerBucketNavRoute(
                onImageClick = { index, file ->
                    navBackStack.add(
                        ImageNavRoute(
                            data = ImageNavRoute.Data.Bucket(
                                imageIndex = index,
                                imageId = file.id,
                                bucketId = file.bucketId,
                            ),
                        ),
                    )
                },
            )
            registerImageNavRoute()
        }
    }
    val saveableStateHolder = rememberSaveableStateHolder()
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
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
                        object: ViewModelStoreOwner,
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
                        LocalViewModelStoreOwner provides childViewModelStoreOwner,
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
    val colorScheme by rememberUpdatedState(MaterialTheme.colorScheme)
    val dimens by rememberUpdatedState(LocalDimens.current)
    SharedTransitionScope { transitionModifier ->
        val sceneState = rememberSceneState(
            entries = navEntries,
            sceneStrategies = listOf(SinglePaneSceneStrategy()),
            sharedTransitionScope = this@SharedTransitionScope,
            onBack = { navBackStack.removeLastOrNull() },
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
                repeat(navEntries.size - currentScene.previousEntries.size) {
                    navBackStack.removeLastOrNull()
                }
            },
        )
        val navResultStore = rememberNavResultStore()
        val rootNavBarController = rememberRootNavBarController()
        Box(modifier = modifier.then(transitionModifier)) {
            CompositionLocalProvider(
                LocalNavResultStore provides navResultStore,
                LocalRootNavBarController provides rootNavBarController,
                LocalNavSharedTransitionScope provides this@SharedTransitionScope,
            ) {
                NavDisplay(
                    sceneState = sceneState,
                    navigationEventState = navEventState,
                    modifier = Modifier.matchParentSize(),
                    transitionSpec = {
                        ContentTransform(
                            targetContentEnter = fadeIn(animationSpec = defaultAnimationSpec()),
                            initialContentExit = fadeOut(animationSpec = defaultAnimationSpec()),
                            sizeTransform = null,
                        )
                    },
                    popTransitionSpec = {
                        ContentTransform(
                            targetContentEnter = fadeIn(animationSpec = defaultAnimationSpec()),
                            initialContentExit = fadeOut(animationSpec = defaultAnimationSpec()),
                            sizeTransform = null,
                        )
                    },
                    predictivePopTransitionSpec = {
                        ContentTransform(
                            targetContentEnter = fadeIn(animationSpec = defaultAnimationSpec()),
                            initialContentExit = fadeOut(animationSpec = defaultAnimationSpec()),
                            sizeTransform = null,
                        )
                    },
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
            ) {
                val navBarAlpha by animateFloatAsState(
                    targetValue =
                        if (currentRouteIsRoot && rootNavBarController.isRootNavBarVisible) {
                            1F
                        } else {
                            0F
                        },
                    animationSpec = defaultAnimationSpec(),
                )
                val navBarVisible by remember {
                    derivedStateOf(structuralEqualityPolicy()) {
                        navBarAlpha > 0F
                    }
                }
                val systemNavBarAlpha by animateFloatAsState(
                    targetValue =
                        if (LocalSystemBarsController.current.isSystemBarsVisible) {
                            1F
                        } else {
                            0F
                        },
                    animationSpec = defaultAnimationSpec(),
                )
                val systemNavBarVisible by remember {
                    derivedStateOf(structuralEqualityPolicy()) {
                        systemNavBarAlpha > 0F
                    }
                }
                if (navBarVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.material3AppBarHeight)
                            .graphicsLayer {
                                alpha = navBarAlpha
                            }
                            .background(
                                color = colorScheme.background.withLowTransparency(),
                                shape = RectangleShape,
                            ),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        for (route in rootRoutes) {
                            val routeSelected by remember {
                                derivedStateOf(structuralEqualityPolicy()) {
                                    route == topRootRoute
                                }
                            }
                            val interactionSource = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .weight(1F)
                                    .fillMaxHeight()
                                    .selectable(
                                        selected = routeSelected,
                                        interactionSource = interactionSource,
                                        indication = null,
                                        role = Role.Tab,
                                        onClick = {
                                            if (routeSelected) {
                                                rootNavBarController.dispatchOnClick(route)
                                            } else {
                                                if (route == initialRoute) {
                                                    navBackStack.clear()
                                                } else {
                                                    val iterator = navBackStack.iterator()
                                                    while (iterator.hasNext()) {
                                                        if (iterator.next() == route) {
                                                            iterator.remove()
                                                        }
                                                    }
                                                }
                                                navBackStack.add(route)
                                            }
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                val backgroundColor by animateColorAsState(
                                    targetValue = if (routeSelected) {
                                        colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    },
                                    animationSpec = defaultAnimationSpec(),
                                )
                                val indicationColor by animateColorAsState(
                                    targetValue = if (routeSelected) {
                                        colorScheme.onPrimary
                                    } else {
                                        colorScheme.onBackground
                                    },
                                    animationSpec = defaultAnimationSpec(),
                                )
                                val selectedIconAlpha by animateFloatAsState(
                                    targetValue = if (routeSelected) 1F else 0F,
                                    animationSpec = defaultAnimationSpec(),
                                )
                                val selectedIconVisible by remember {
                                    derivedStateOf(structuralEqualityPolicy()) {
                                        selectedIconAlpha > 0F
                                    }
                                }
                                val unselectedIconAlpha by animateFloatAsState(
                                    targetValue = if (routeSelected) 0F else 1F,
                                    animationSpec = defaultAnimationSpec(),
                                )
                                val unselectedIconVisible by remember {
                                    derivedStateOf(structuralEqualityPolicy()) {
                                        unselectedIconAlpha > 0F
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(
                                            width = dimens.material3NavBarIndicatorWidth,
                                            height = dimens.material3NavBarIndicatorHeight,
                                        )
                                        .clip(CircleShape)
                                        .drawBehind {
                                            drawRect(color = backgroundColor)
                                        }
                                        .indication(
                                            interactionSource = interactionSource,
                                            indication = ripple(color = { indicationColor }),
                                        ),
                                )
                                if (selectedIconVisible) {
                                    Icon(
                                        painter = painterResource(route.selectedIconRes),
                                        contentDescription = stringResource(route.titleRes),
                                        tint = colorScheme.onPrimary,
                                        modifier = Modifier.graphicsLayer {
                                            alpha = selectedIconAlpha
                                        },
                                    )
                                }
                                if (unselectedIconVisible) {
                                    Icon(
                                        painter = painterResource(route.unselectedIconRes),
                                        contentDescription = stringResource(route.titleRes),
                                        tint = colorScheme.onBackground,
                                        modifier = Modifier.graphicsLayer {
                                            alpha = unselectedIconAlpha
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                if (systemNavBarVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding(),
                            )
                            .graphicsLayer {
                                alpha = systemNavBarAlpha
                            }
                            .background(
                                color = colorScheme.background.withLowTransparency(),
                                shape = RectangleShape,
                            ),
                    )
                }
            }
        }
    }
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

    private val viewModelStores: MutableMap<Any, ViewModelStore> = LinkedHashMap()
}

@Composable
private fun rememberRootNavBarController(): RootNavBarControllerImpl =
    rememberSaveable(saver = RootNavBarControllerImplSaver) { RootNavBarControllerImpl() }

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

    private val listeners: MutableMap<RootNavRoute, () -> Unit> = LinkedHashMap()
}

@Parcelize
private data class RootNavBarConfig(val isVisible: Boolean): Parcelable

private object RootNavBarControllerImplSaver: Saver<RootNavBarControllerImpl, RootNavBarConfig> {

    override fun SaverScope.save(value: RootNavBarControllerImpl): RootNavBarConfig =
        RootNavBarConfig(isVisible = value.isRootNavBarVisible)

    override fun restore(value: RootNavBarConfig): RootNavBarControllerImpl {
        val controller = RootNavBarControllerImpl()
        controller.isRootNavBarVisible = value.isVisible
        return controller
    }
}

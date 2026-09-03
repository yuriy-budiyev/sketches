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
import android.view.View
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.SecureFlagPolicy
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
import com.github.yuriybudiyev.sketches.core.navigation.LocalRootNavMenuController
import com.github.yuriybudiyev.sketches.core.navigation.NavRoute
import com.github.yuriybudiyev.sketches.core.navigation.RootNavMenuController
import com.github.yuriybudiyev.sketches.core.navigation.RootNavRoute
import com.github.yuriybudiyev.sketches.core.navigation.rememberNavResultStore
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.OnRequestMediaAccess
import com.github.yuriybudiyev.sketches.core.platform.systembars.LocalSystemBarsController
import com.github.yuriybudiyev.sketches.core.saveable.rememberSaveableSnapshotStateList
import com.github.yuriybudiyev.sketches.core.ui.animation.DefaultAnimatedVisibility
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultAnimationSpec
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultEnterTransition
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultExitTransition
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens
import com.github.yuriybudiyev.sketches.core.ui.theme.rememberBottomToTopBackgroundGradientBrush
import com.github.yuriybudiyev.sketches.core.ui.theme.withLowTransparency
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val navResultStore = rememberNavResultStore()
    val navMenuController = rememberRootNavMenuController()
    val systemBarsController by rememberUpdatedState(LocalSystemBarsController.current)
    val colorScheme = MaterialTheme.colorScheme
    val dimens = LocalDimens.current
    val navMenuVisible by remember {
        derivedStateOf(structuralEqualityPolicy()) {
            navBackStack.lastOrNull() is RootNavRoute &&
                navMenuController.isNavMenuVisible
        }
    }
    val navBarVisible by remember {
        derivedStateOf(structuralEqualityPolicy()) {
            navBackStack.lastOrNull() is RootNavRoute &&
                systemBarsController.isSystemBarsVisible
        }
    }
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
        Box(modifier = modifier.then(transitionModifier)) {
            CompositionLocalProvider(
                LocalNavResultStore provides navResultStore,
                LocalRootNavMenuController provides navMenuController,
                LocalNavSharedTransitionScope provides this@SharedTransitionScope,
            ) {
                NavDisplay(
                    sceneState = sceneState,
                    navigationEventState = navEventState,
                    modifier = Modifier.matchParentSize(),
                    transitionSpec = {
                        ContentTransform(
                            targetContentEnter = defaultEnterTransition(),
                            initialContentExit = defaultExitTransition(),
                            sizeTransform = null,
                        )
                    },
                    popTransitionSpec = {
                        ContentTransform(
                            targetContentEnter = defaultEnterTransition(),
                            initialContentExit = defaultExitTransition(),
                            sizeTransform = null,
                        )
                    },
                    predictivePopTransitionSpec = {
                        ContentTransform(
                            targetContentEnter = defaultEnterTransition(),
                            initialContentExit = defaultExitTransition(),
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
                DefaultAnimatedVisibility(navMenuVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.navBarHeight)
                            .background(
                                color = colorScheme.background.withLowTransparency(),
                                shape = RectangleShape,
                            ),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        for (route in rootRoutes) {
                            val routeSelected by remember {
                                derivedStateOf(structuralEqualityPolicy()) {
                                    route == navBackStack.lastOrNull()
                                }
                            }
                            NavItem(
                                route = route,
                                selected = routeSelected,
                                onClick = {
                                    if (routeSelected) {
                                        navMenuController.dispatchOnClick(route)
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
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1F),
                            )
                        }
                    }
                }
                DefaultAnimatedVisibility(navBarVisible) {
                    val bottomNavBarHeight =
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    if (bottomNavBarHeight > 0.dp) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(bottomNavBarHeight)
                                .background(
                                    brush = rememberBottomToTopBackgroundGradientBrush(colorScheme),
                                    shape = RectangleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    route: RootNavRoute,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onClick by rememberUpdatedState(onClick)
    val coroutineScope = rememberCoroutineScope()
    val hintPositionProvider = remember { HintPositionProvider() }
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes
    val dimens = LocalDimens.current
    val backgroundColor by animateColorAsState(
        targetValue =
            if (selected) {
                colorScheme.primary
            } else {
                Color.Transparent
            },
        animationSpec = defaultAnimationSpec(),
    )
    val indicationColor by animateColorAsState(
        targetValue =
            if (selected) {
                colorScheme.onPrimary
            } else {
                colorScheme.onBackground
            },
        animationSpec = defaultAnimationSpec(),
    )
    var hintVisible by remember { mutableStateOf(false) }
    val hintAlpha by animateFloatAsState(
        targetValue = if (hintVisible) 1F else 0F,
        animationSpec = defaultAnimationSpec(),
    )
    val hintInComposition by remember {
        derivedStateOf(structuralEqualityPolicy()) {
            hintVisible || hintAlpha > 0F
        }
    }
    val title = stringResource(route.titleRes)
    Box(
        modifier = modifier
            .semantics {
                also { properties ->
                    properties.role = Role.Tab
                    properties.selected = selected
                    properties.contentDescription = title
                }
            }
            .pointerInput(Unit) {
                var hideJob: Job? = null
                detectTapGestures(
                    onPress = { offset ->
                        val press = PressInteraction.Press(offset)
                        coroutineScope.launch {
                            interactionSource.emit(press)
                        }
                        val released = tryAwaitRelease()
                        coroutineScope.launch {
                            interactionSource.emit(
                                if (released) {
                                    PressInteraction.Release(press)
                                } else {
                                    PressInteraction.Cancel(press)
                                },
                            )
                        }
                        hideJob?.cancel()
                        hideJob = coroutineScope.launch {
                            delay(timeMillis = 1500L)
                            hintVisible = false
                        }
                    },
                    onLongPress = {
                        hideJob?.cancel()
                        coroutineScope.launch {
                            hintVisible = true
                        }
                    },
                    onTap = {
                        coroutineScope.launch {
                            onClick()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.wrapContentSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (hintInComposition) {
                Popup(
                    popupPositionProvider = hintPositionProvider,
                    onDismissRequest = {
                        hintVisible = false
                    },
                    properties = PopupProperties(
                        focusable = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        securePolicy = SecureFlagPolicy.Inherit,
                        excludeFromSystemGesture = true,
                        clippingEnabled = true,
                    ),
                ) {
                    val popupView = LocalView.current
                    SideEffect(popupView) {
                        var view: View? = popupView
                        while (view != null) {
                            view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            view = view.parent as? View
                        }
                    }
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = hintAlpha
                            }
                            .padding(all = 8.dp)
                            .dropShadow(
                                shape = shapes.extraSmall,
                                shadow = Shadow(
                                    radius = dimens.shadowBlurRadius,
                                    color = colorScheme.scrim.withLowTransparency(),
                                ),
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = colorScheme.surfaceContainerHigh,
                                    shape = shapes.extraSmall,
                                )
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp,
                                ),
                        ) {
                            Text(
                                text = title,
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(
                        width = dimens.navBarIndicatorWidth,
                        height = dimens.navBarIndicatorHeight,
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
            DefaultAnimatedVisibility(selected) {
                Icon(
                    painter = painterResource(route.selectedIconRes),
                    contentDescription = stringResource(route.titleRes),
                    tint = colorScheme.onPrimary,
                )
            }
            DefaultAnimatedVisibility(!selected) {
                Icon(
                    painter = painterResource(route.unselectedIconRes),
                    contentDescription = stringResource(route.titleRes),
                    tint = colorScheme.onBackground,
                )
            }
        }
    }
}

private class HintPositionProvider: PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        when {
            x < 0 -> {
                x = 0
            }
            x + popupContentSize.width > windowSize.width -> {
                x = windowSize.width - popupContentSize.width
            }
        }
        var y = anchorBounds.top - popupContentSize.height
        if (y < 0) {
            y = anchorBounds.bottom
        }
        return IntOffset(x, y)
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
private fun rememberRootNavMenuController(): RootNavMenuControllerImpl =
    rememberSaveable(saver = RootNavMenuControllerImplSaver) { RootNavMenuControllerImpl() }

private class RootNavMenuControllerImpl: RootNavMenuController {

    override var isNavMenuVisible: Boolean by mutableStateOf(true)

    override fun showNavMenu() {
        isNavMenuVisible = true
    }

    override fun hideNavMenu() {
        isNavMenuVisible = false
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
private data class RootNavMenuConfig(val isNavMenuVisible: Boolean): Parcelable

private object RootNavMenuControllerImplSaver: Saver<RootNavMenuControllerImpl, RootNavMenuConfig> {

    override fun SaverScope.save(value: RootNavMenuControllerImpl): RootNavMenuConfig =
        RootNavMenuConfig(isNavMenuVisible = value.isNavMenuVisible)

    override fun restore(value: RootNavMenuConfig): RootNavMenuControllerImpl {
        val controller = RootNavMenuControllerImpl()
        controller.isNavMenuVisible = value.isNavMenuVisible
        return controller
    }
}

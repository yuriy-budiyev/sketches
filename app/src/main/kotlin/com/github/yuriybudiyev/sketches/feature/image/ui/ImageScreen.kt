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

package com.github.yuriybudiyev.sketches.feature.image.ui

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaFile
import com.github.yuriybudiyev.sketches.core.navigation.LocalNavResultStore
import com.github.yuriybudiyev.sketches.core.platform.bars.LocalSystemBarsController
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import com.github.yuriybudiyev.sketches.core.platform.content.launchDeleteMediaRequest
import com.github.yuriybudiyev.sketches.core.platform.share.LocalShareManager
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultAnimationSpec
import com.github.yuriybudiyev.sketches.core.ui.colors.LowTransparencyAlpha
import com.github.yuriybudiyev.sketches.core.ui.colors.NoTransparencyAlpha
import com.github.yuriybudiyev.sketches.core.ui.colors.withHighTransparency
import com.github.yuriybudiyev.sketches.core.ui.colors.withLowTransparency
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAppBarActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesDeleteBookmarksConfirmationDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesDeleteImagesConfirmationDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.components.ZoomState
import com.github.yuriybudiyev.sketches.core.ui.components.media.SketchesPreviewAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.components.media.SketchesThumbnailAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.components.media.player.SketchesMediaPlayer
import com.github.yuriybudiyev.sketches.core.ui.components.media.player.rememberSketchesMediaState
import com.github.yuriybudiyev.sketches.core.ui.components.rememberZoomState
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens
import com.github.yuriybudiyev.sketches.core.ui.scroll.scrollToItemCentered
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageScreenNavResult
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Composable
fun ImageRoute(viewModel: ImageScreenViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateMediaAccess()
    }
    val navResultStore by rememberUpdatedState(LocalNavResultStore.current)
    ImageScreen(
        uiState = uiState,
        onChange = { index, file ->
            val fileId = file.id
            viewModel.setCurrentFileInfo(
                fileIndex = index,
                fileId = fileId,
            )
            navResultStore.putNavResult(
                result = ImageScreenNavResult(
                    fileIndex = index,
                    fileId = fileId,
                ),
            )
        },
        onDeleteImage = { _, file ->
            viewModel.deleteMedia(listOf(file.uri))
        },
        onCreateBookmark = { mediaId ->
            viewModel.createBookmark(mediaId)
        },
        onDeleteBookmark = { mediaId ->
            viewModel.deleteBookmark(mediaId)
        },
    )
}

@Composable
fun ImageScreen(
    uiState: ImageScreenViewModel.UiState,
    onChange: (index: Int, file: MediaFile) -> Unit,
    onDeleteImage: (index: Int, file: MediaFile) -> Unit,
    onCreateBookmark: (mediaId: Long) -> Unit,
    onDeleteBookmark: (mediaId: Long) -> Unit,
) {
    val onBackPressedDispatcher by rememberUpdatedState(
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher,
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        when (uiState) {
            is ImageScreenViewModel.UiState.Empty -> {
                LaunchedEffect(Unit) {
                    onBackPressedDispatcher?.onBackPressed()
                }
            }
            is ImageScreenViewModel.UiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is ImageScreenViewModel.UiState.Image -> {
                ImageScreenLayout(
                    index = uiState.index,
                    files = uiState.files,
                    onChange = onChange,
                    onDeleteImage = onDeleteImage,
                    onCreateBookmark = onCreateBookmark,
                    onDeleteBookmark = onDeleteBookmark,
                    modifier = Modifier.matchParentSize(),
                )
            }
            is ImageScreenViewModel.UiState.Error -> {
                SketchesErrorMessage(
                    thrown = uiState.thrown,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}

@Composable
private fun ImageScreenLayout(
    index: Int,
    files: List<MediaFile>,
    onChange: (index: Int, file: MediaFile) -> Unit,
    onDeleteImage: (index: Int, file: MediaFile) -> Unit,
    onCreateBookmark: (mediaId: Long) -> Unit,
    onDeleteBookmark: (mediaId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentIndex by rememberSaveable { mutableIntStateOf(index) }.apply {
        val maxIndex = files.size - 1
        if (intValue > maxIndex) {
            intValue = maxIndex
        }
    }
    val files by rememberUpdatedState(files)
    val context by rememberUpdatedState(LocalContext.current)
    val shareManager by rememberUpdatedState(LocalShareManager.current)
    val onChange by rememberUpdatedState(onChange)
    val onDeleteImage by rememberUpdatedState(onDeleteImage)
    val onCreateBookmark by rememberUpdatedState(onCreateBookmark)
    val onDeleteBookmark by rememberUpdatedState(onDeleteBookmark)
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(currentIndex) { files.size }
    val barState = rememberLazyListState(currentIndex)
    val systemBarsController by rememberUpdatedState(LocalSystemBarsController.current)
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { },
    )
    var deleteImageDialogVisible by rememberSaveable { mutableStateOf(false) }
    var deleteBookmarkDialogVisible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(index) {
        pagerState.scrollToPage(index)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            currentIndex = page
            onChange(
                page,
                files[page],
            )
            coroutineScope.launch {
                barState.scrollToItemCentered(
                    index = page,
                    animate = true,
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { systemBarsController.isSystemBarsVisible }.collect { visible ->
            if (visible) {
                coroutineScope.launch {
                    barState.scrollToItemCentered(
                        index = pagerState.currentPage,
                        animate = false,
                    )
                }
            }
        }
    }
    val colorScheme by rememberUpdatedState(MaterialTheme.colorScheme)
    val dimens by rememberUpdatedState(LocalDimens.current)
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    Box(modifier = modifier.onSizeChanged { size -> containerSize = size }) {
        val layoutDirection = LocalLayoutDirection.current
        var contentInsets = WindowInsets.navigationBars
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        val landscapeScreenOrientation =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (landscapeScreenOrientation) {
            contentInsets = contentInsets
                .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
        }
        val contentPaddings = contentInsets.asPaddingValues()
        var contentPaddingStartVisible by remember { mutableStateOf(0.dp) }.apply {
            val newValue = contentPaddings.calculateStartPadding(layoutDirection)
            if (newValue > value) {
                value = newValue
            }
        }
        var contentPaddingEndVisible by remember { mutableStateOf(0.dp) }.apply {
            val newValue = contentPaddings.calculateEndPadding(layoutDirection)
            if (newValue > value) {
                value = newValue
            }
        }
        var contentPaddingBottomVisible by remember { mutableStateOf(0.dp) }.apply {
            val newValue = contentPaddings.calculateBottomPadding()
            if (newValue > value) {
                value = newValue
            }
        }
        LaunchedEffect(Unit) {
            snapshotFlow { containerSize }.collect {
                contentPaddingStartVisible = 0.dp
                contentPaddingEndVisible = 0.dp
                contentPaddingBottomVisible = 0.dp
            }
        }
        val systemBarsVisible = systemBarsController.isSystemBarsVisible
        val contentPaddingStart by animateDpAsState(
            targetValue = if (systemBarsVisible) {
                contentPaddingStartVisible
            } else {
                0.dp
            },
            animationSpec = defaultAnimationSpec(),
        )
        val contentPaddingEnd by animateDpAsState(
            targetValue = if (systemBarsVisible) {
                contentPaddingEndVisible
            } else {
                0.dp
            },
            animationSpec = defaultAnimationSpec(),
        )
        val contentPaddingBottom by animateDpAsState(
            targetValue = if (systemBarsVisible) {
                contentPaddingBottomVisible
            } else {
                0.dp
            },
            animationSpec = defaultAnimationSpec(),
        )
        val controllerPaddingBottom by animateDpAsState(
            targetValue = if (systemBarsVisible) {
                dimens.mediaBarHeight
            } else {
                0.dp
            },
            animationSpec = defaultAnimationSpec(),
        )
        MediaPager(
            state = pagerState,
            files = files,
            onPageTap = {
                coroutineScope.launch {
                    if (systemBarsVisible) {
                        systemBarsController.hideSystemBars()
                    } else {
                        systemBarsController.showSystemBars()
                    }
                }
            },
            controllerVisible = systemBarsVisible,
            controllerStartPadding = contentPaddingStart,
            controllerEndPadding = contentPaddingEnd,
            controllerBottomPadding = contentPaddingBottom + controllerPaddingBottom,
            modifier = Modifier.matchParentSize(),
        )
        val uiAlpha by animateFloatAsState(
            targetValue = if (systemBarsVisible) 1F else 0F,
            animationSpec = defaultAnimationSpec(),
        )
        val mediaBarOffset by animateIntOffsetAsState(
            targetValue = if (systemBarsVisible) {
                IntOffset.Zero
            } else {
                IntOffset(
                    x = 0,
                    y = with(LocalDensity.current) { dimens.mediaBarHeight.roundToPx() },
                )
            },
            animationSpec = defaultAnimationSpec(),
        )
        val topAppBarOffset by animateIntOffsetAsState(
            targetValue = if (systemBarsVisible) {
                IntOffset.Zero
            } else {
                IntOffset(
                    x = 0,
                    y = with(LocalDensity.current) { -dimens.material3AppBarHeight.roundToPx() },
                )
            },
            animationSpec = defaultAnimationSpec(),
        )
        if (uiAlpha > 0F) {
            MediaBar(
                currentIndex = currentIndex,
                state = barState,
                files = files,
                onItemClick = { index, _ ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(
                            page = index,
                            animationSpec = defaultAnimationSpec(),
                        )
                    }
                },
                modifier = Modifier
                    .offset { mediaBarOffset }
                    .align(Alignment.BottomStart)
                    .padding(
                        start = contentPaddingStart,
                        end = contentPaddingEnd,
                        bottom = contentPaddingBottom,
                    )
                    .graphicsLayer {
                        alpha = uiAlpha
                    }
                    .background(
                        color = colorScheme.background.withLowTransparency(),
                        shape = RectangleShape,
                    )
                    .height(dimens.mediaBarHeight)
                    .fillMaxWidth(),
            )
            SketchesTopAppBar(
                modifier = Modifier
                    .offset { topAppBarOffset }
                    .align(Alignment.TopStart)
                    .padding(
                        start = contentPaddingStart,
                        end = contentPaddingEnd,
                    )
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = uiAlpha
                    },
                text = files[currentIndex].name,
                backgroundColor = colorScheme.background.withLowTransparency(),
                windowInsets =
                    WindowInsets.statusBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Top),
            ) {
                val hasBookmark by remember {
                    derivedStateOf(structuralEqualityPolicy()) {
                        files[currentIndex].bookmark != null
                    }
                }
                SketchesAppBarActionButton(
                    iconRes =
                        if (hasBookmark) {
                            R.drawable.ic_bookmark_delete
                        } else {
                            R.drawable.ic_bookmark_create
                        },
                    description = stringResource(
                        if (hasBookmark) {
                            R.string.delete_bookmark
                        } else {
                            R.string.create_bookmark
                        },
                    ),
                    onClick = {
                        val file = files[currentIndex]
                        if (file.bookmark != null) {
                            deleteBookmarkDialogVisible = true
                        } else {
                            onCreateBookmark(file.id)
                        }
                    },
                )
                SketchesAppBarActionButton(
                    iconRes = R.drawable.ic_delete,
                    description = stringResource(R.string.delete_image),
                    onClick = {
                        deleteImageDialogVisible = true
                    },
                )
                val shareDescription = stringResource(R.string.share_image)
                SketchesAppBarActionButton(
                    iconRes = R.drawable.ic_share,
                    description = shareDescription,
                    onClick = {
                        coroutineScope.launch {
                            val file = files[currentIndex]
                            shareManager.startChooserActivity(
                                file.uri,
                                file.mimeType,
                                shareDescription,
                            )
                        }
                    },
                )
            }
            if (contentPaddingStart > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxHeight()
                        .width(contentPaddingStart)
                        .graphicsLayer {
                            alpha = uiAlpha
                        }
                        .background(
                            color = colorScheme.background.withLowTransparency(),
                            shape = RectangleShape,
                        ),
                )
            }
            if (contentPaddingEnd > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .width(contentPaddingEnd)
                        .graphicsLayer {
                            alpha = uiAlpha
                        }
                        .background(
                            color = colorScheme.background.withLowTransparency(),
                            shape = RectangleShape,
                        ),
                )
            }
            if (contentPaddingBottom > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(contentPaddingBottom)
                        .graphicsLayer {
                            alpha = uiAlpha
                        }
                        .background(
                            color = colorScheme.background.withLowTransparency(),
                            shape = RectangleShape,
                        ),
                )
            }
        }
        if (deleteImageDialogVisible) {
            SketchesDeleteImagesConfirmationDialog(
                count = 1,
                onDelete = {
                    deleteImageDialogVisible = false
                    coroutineScope.launch {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            deleteRequestLauncher.launchDeleteMediaRequest(
                                context,
                                listOf(files[currentIndex].uri),
                            )
                        } else {
                            onDeleteImage(
                                currentIndex,
                                files[currentIndex],
                            )
                        }
                    }
                },
                onDismiss = {
                    deleteImageDialogVisible = false
                },
            )
        }
        if (deleteBookmarkDialogVisible) {
            SketchesDeleteBookmarksConfirmationDialog(
                count = 1,
                onDelete = {
                    deleteBookmarkDialogVisible = false
                    coroutineScope.launch {
                        onDeleteBookmark(files[currentIndex].id)
                    }
                },
                onDismiss = {
                    deleteBookmarkDialogVisible = false
                },
            )
        }
    }
}

@Composable
private fun MediaPager(
    state: PagerState,
    files: List<MediaFile>,
    onPageTap: () -> Unit,
    controllerVisible: Boolean,
    controllerStartPadding: Dp,
    controllerEndPadding: Dp,
    controllerBottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val files by rememberUpdatedState(files)
    val onPageTap by rememberUpdatedState(onPageTap)
    val controllerVisible by rememberUpdatedState(controllerVisible)
    val controllerStartPadding by rememberUpdatedState(controllerStartPadding)
    val controllerEndPadding by rememberUpdatedState(controllerEndPadding)
    val controllerBottomPadding by rememberUpdatedState(controllerBottomPadding)
    HorizontalPager(
        state = state,
        key = { page -> files[page].id },
        flingBehavior = PagerDefaults.flingBehavior(
            state = state,
            snapAnimationSpec = defaultAnimationSpec(),
        ),
        modifier = modifier,
    ) { page ->
        val file = files[page]
        MediaPage(
            state = state,
            number = page,
            fileUri = file.uri,
            fileType = file.mediaType,
            onPageTap = onPageTap,
            controllerVisible = controllerVisible,
            controllerStartPadding = controllerStartPadding,
            controllerEndPadding = controllerEndPadding,
            controllerBottomPadding = controllerBottomPadding,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun MediaPage(
    state: PagerState,
    number: Int,
    fileUri: Uri,
    fileType: MediaType,
    onPageTap: () -> Unit,
    controllerVisible: Boolean,
    controllerStartPadding: Dp,
    controllerEndPadding: Dp,
    controllerBottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val zoomState = rememberZoomState()
    LaunchedEffect(
        state,
        number,
        zoomState,
    ) {
        snapshotFlow { state.currentPage }.collect { currentPage ->
            if (currentPage != number) {
                if (zoomState.isZoomed) {
                    coroutineScope.launch {
                        zoomState.toggleZoom(animate = false)
                    }
                }
            }
        }
    }
    BackHandler(zoomState.isZoomed) {
        coroutineScope.launch {
            zoomState.toggleZoom(animate = true)
        }
    }
    when (fileType) {
        MediaType.Image -> {
            ImagePage(
                state = state,
                zoomState = zoomState,
                number = number,
                fileUri = fileUri,
                onPageTap = onPageTap,
                modifier = modifier,
            )
        }
        MediaType.Video -> {
            VideoPage(
                state = state,
                zoomState = zoomState,
                number = number,
                fileUri = fileUri,
                onPageTap = onPageTap,
                controllerVisible = controllerVisible,
                controllerStartPadding = controllerStartPadding,
                controllerEndPadding = controllerEndPadding,
                controllerBottomPadding = controllerBottomPadding,
                modifier = modifier,
            )
        }
    }
}

@Composable
@NonRestartableComposable
private fun ImagePage(
    state: PagerState,
    zoomState: ZoomState,
    number: Int,
    fileUri: Uri,
    onPageTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val number by rememberUpdatedState(number)
    var displayedPage by remember { mutableStateOf(state.currentPage == number) }
    LaunchedEffect(state) {
        snapshotFlow { state.currentPage }.collect { currentPage ->
            displayedPage = currentPage == number
        }
    }
    SketchesPreviewAsyncImage(
        uri = fileUri,
        contentDescription = stringResource(R.string.image),
        onTap = onPageTap,
        modifier = modifier,
        zoomState = zoomState,
    )
}

@Composable
private fun VideoPage(
    state: PagerState,
    zoomState: ZoomState,
    number: Int,
    fileUri: Uri,
    onPageTap: () -> Unit,
    controllerVisible: Boolean,
    controllerStartPadding: Dp,
    controllerEndPadding: Dp,
    controllerBottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val number by rememberUpdatedState(number)
    val mediaState = rememberSketchesMediaState()
    DisposableEffect(fileUri) {
        mediaState.open(fileUri)
        onDispose {
            mediaState.close()
            mediaState.release()
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.currentPage }.collect { currentPage ->
            if (currentPage == number) {
                mediaState.coroutineScope.launch {
                    if (mediaState.isVolumeEnabled) {
                        mediaState.disableVolume()
                    }
                    if (!mediaState.isPlaying) {
                        mediaState.playIfNotPlayed()
                    }
                }
            } else {
                mediaState.coroutineScope.launch {
                    if (mediaState.isPlaying) {
                        mediaState.pause()
                    }
                    if (mediaState.isVolumeEnabled) {
                        mediaState.disableVolume()
                    }
                    mediaState.resetNotPlayed()
                }
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        mediaState.coroutineScope.launch {
            mediaState.pause()
        }
    }
    SketchesMediaPlayer(
        state = mediaState,
        onDisplayTap = onPageTap,
        controllerVisible = controllerVisible,
        controllerStartPadding = controllerStartPadding,
        controllerEndPadding = controllerEndPadding,
        controllerBottomPadding = controllerBottomPadding,
        modifier = modifier,
        zoomState = zoomState,
        enablePlaceholder = true,
        enableErrorIndicator = true,
    )
}

@Composable
private fun MediaBar(
    currentIndex: Int,
    state: LazyListState,
    files: List<MediaFile>,
    onItemClick: (index: Int, file: MediaFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val currentIndex by rememberUpdatedState(currentIndex)
    val files by rememberUpdatedState(files)
    val onItemClick by rememberUpdatedState(onItemClick)
    val colorScheme by rememberUpdatedState(MaterialTheme.colorScheme)
    val dimens by rememberUpdatedState(LocalDimens.current)
    LazyRow(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = dimens.mediaBarItemSpacing),
        horizontalArrangement = Arrangement.spacedBy(
            space = dimens.mediaBarItemSpacing,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(
            count = files.size,
            key = { position -> MediaBarKey(files[position].id) },
        ) { position ->
            val file = files[position]
            Box(
                modifier = Modifier
                    .animateItem()
                    .size(size = dimens.mediaBarItemSize)
                    .border(
                        width = dimens.mediaItemBorderThickness,
                        color = if (position == currentIndex) {
                            colorScheme.onBackground.withLowTransparency()
                        } else {
                            colorScheme.onBackground.withHighTransparency()
                        },
                        shape = RectangleShape,
                    )
                    .clipToBounds()
                    .clickable {
                        coroutineScope.launch {
                            onItemClick(
                                position,
                                file,
                            )
                        }
                    },
            ) {
                val fileUri = file.uri
                SketchesThumbnailAsyncImage(
                    uri = fileUri,
                    contentDescription = stringResource(
                        id = when (file.mediaType) {
                            MediaType.Image -> R.string.image
                            MediaType.Video -> R.string.video
                        },
                    ),
                    modifier = Modifier
                        .graphicsLayer(
                            alpha = if (position == currentIndex) {
                                NoTransparencyAlpha
                            } else {
                                LowTransparencyAlpha
                            },
                        )
                        .matchParentSize(),
                )
                if (file.mediaType == MediaType.Video) {
                    Icon(
                        painter = painterResource(R.drawable.ic_video),
                        contentDescription = stringResource(R.string.video),
                        tint = colorScheme.onBackground,
                        modifier = Modifier
                            .align(alignment = Alignment.BottomStart)
                            .padding(all = dimens.mediaBarVideoIconPadding)
                            .background(
                                color = colorScheme.background.withLowTransparency(),
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Immutable
@Parcelize
private data class MediaBarKey(val fileId: Long): Parcelable

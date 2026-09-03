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

package com.github.yuriybudiyev.sketches.feature.bucket.ui

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.component1
import androidx.activity.result.component2
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaFile
import com.github.yuriybudiyev.sketches.core.navigation.LocalNavResultStore
import com.github.yuriybudiyev.sketches.core.platform.content.launchDeleteMediaRequest
import com.github.yuriybudiyev.sketches.core.platform.share.LocalShareManager
import com.github.yuriybudiyev.sketches.core.platform.systembars.LocalSystemBarsController
import com.github.yuriybudiyev.sketches.core.saveable.rememberSaveableSnapshotStateSet
import com.github.yuriybudiyev.sketches.core.ui.animation.DefaultAlphaAnimationSpec
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesDeleteImagesConfirmationDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.components.media.SketchesMediaGrid
import com.github.yuriybudiyev.sketches.core.ui.components.media.SketchesMediaGridContentType
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.BatchAction
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.MediaBatchState
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.rememberMediaBatchState
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.toMediaDescriptorList
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.toUriList
import com.github.yuriybudiyev.sketches.core.ui.components.media.share.prepareForSharing
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens
import com.github.yuriybudiyev.sketches.core.ui.theme.rememberBottomToTopBackgroundGradientBrush
import com.github.yuriybudiyev.sketches.core.ui.theme.withLowTransparency
import com.github.yuriybudiyev.sketches.core.ui.utils.rememberLastScrolledScrollConnection
import com.github.yuriybudiyev.sketches.core.ui.utils.scrollToItem
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageScreenNavResult
import kotlinx.coroutines.launch

@Composable
fun BucketRoute(
    viewModel: BucketScreenViewModel,
    onImageClick: (index: Int, file: MediaFile) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateMediaAccess()
    }
    BucketScreen(
        bucketName = viewModel.bucketName,
        uiState = uiState,
        onImageClick = onImageClick,
        onDeleteMedia = { files ->
            viewModel.deleteMedia(files)
        },
        onShowBucket = {
            viewModel.showBucket()
        },
        onHideBucket = {
            viewModel.hideBucket()
        },
    )
}

@Composable
fun BucketScreen(
    bucketName: String,
    uiState: BucketScreenViewModel.UiState,
    onImageClick: (index: Int, file: MediaFile) -> Unit,
    onDeleteMedia: (files: Collection<Uri>) -> Unit,
    onShowBucket: () -> Unit,
    onHideBucket: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context by rememberUpdatedState(LocalContext.current)
    val shareManager by rememberUpdatedState(LocalShareManager.current)
    val onDeleteMedia by rememberUpdatedState(onDeleteMedia)
    var allFiles by remember { mutableStateOf<Collection<MediaFile>>(emptyList()) }
    val selectedFiles = rememberSaveableSnapshotStateSet<Long>()
    var deleteDialogVisible by rememberSaveable { mutableStateOf(false) }
    var bucketHidden by rememberSaveable { mutableStateOf(false) }
    val mediaGridState = rememberLazyGridState()
    val mediaGridScrollConnection = rememberLastScrolledScrollConnection(mediaGridState)
    val mediaBatchState = rememberMediaBatchState()
    var currentBatch by rememberSaveable { mutableStateOf<Set<Long>>(emptySet()) }
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { (resultCode, _) ->
            coroutineScope.launch {
                if (resultCode == Activity.RESULT_OK) {
                    selectedFiles.removeAll(currentBatch)
                    mediaBatchState.proceed()
                } else {
                    mediaBatchState.reset()
                }
            }
        },
    )
    LaunchedEffect(Unit) {
        mediaBatchState.action.collect { action ->
            when (action) {
                is MediaBatchState.Action.Batch -> {
                    currentBatch = action.ids
                    when (action.payload) {
                        is BatchAction.Share -> {
                            shareManager.startChooserActivity(
                                uris = action.uris,
                                mimeType = action.payload.mimeType,
                                chooserTitle = action.payload.chooserTitle,
                                listenerAction = ShareAction,
                            )
                        }
                        is BatchAction.Delete -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                deleteRequestLauncher.launchDeleteMediaRequest(
                                    context,
                                    action.uris,
                                )
                            } else {
                                error("Low SDK version: ${Build.VERSION.SDK_INT}")
                            }
                        }
                    }
                }
                is MediaBatchState.Action.Finish -> {
                    selectedFiles.clear()
                    currentBatch = emptySet()
                }
                is MediaBatchState.Action.Reset -> {
                    currentBatch = emptySet()
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { selectedFiles.isEmpty() }.collect { empty ->
            if (empty) {
                coroutineScope.launch {
                    mediaBatchState.reset()
                }
            }
        }
    }
    DisposableEffect(shareManager) {
        val shareManager = shareManager
        shareManager.registerOnSharedListener(ShareAction) {
            coroutineScope.launch {
                selectedFiles.clear()
            }
        }
        onDispose {
            shareManager.unregisterOnSharedListener(ShareAction)
        }
    }
    DisposableEffect(shareManager) {
        shareManager.registerOnSharedListener(ShareAction) {
            coroutineScope.launch {
                selectedFiles.removeAll(currentBatch)
                mediaBatchState.reset()
                if (allFiles.isNotEmpty() && selectedFiles.isNotEmpty()) {
                    val bucketIndex = allFiles.indexOfFirst { file ->
                        selectedFiles.contains(file.id)
                    }
                    if (bucketIndex != -1) {
                        mediaGridState.scrollToItem(
                            index = bucketIndex,
                            itemType = null,
                            animate = false,
                            snapToClosestEdge = false,
                            onlyIfItemAtIndexIsNotVisible = true,
                        )
                    }
                }
            }
        }
        onDispose {
            shareManager.unregisterOnSharedListener(ShareAction)
        }
    }
    LaunchedEffect(Unit) {
        if (selectedFiles.isEmpty()) {
            deleteDialogVisible = false
        }
    }
    BackHandler(selectedFiles.isNotEmpty()) {
        coroutineScope.launch {
            selectedFiles.clear()
        }
    }
    val navResultStore = LocalNavResultStore.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(
        navResultStore,
        lifecycleOwner,
    ) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navResultStore.collectNavResult<ImageScreenNavResult> { result ->
                mediaGridState.scrollToItem(
                    index = result.fileIndex,
                    itemType = SketchesMediaGridContentType.MediaStoreFile,
                    animate = false,
                    snapToClosestEdge = true,
                    onlyIfItemAtIndexIsNotVisible = true,
                )
            }
        }
    }
    val systemBarsController = LocalSystemBarsController.current
    LaunchedEffect(
        systemBarsController,
        lifecycleOwner,
    ) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (!systemBarsController.isSystemBarsVisible) {
                systemBarsController.showSystemBars()
            }
        }
    }
    val onBackPressedDispatcher by rememberUpdatedState(
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher,
    )
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorScheme.background),
    ) {
        when (uiState) {
            is BucketScreenViewModel.UiState.Empty -> {
                LaunchedEffect(Unit) {
                    mediaGridScrollConnection.reset()
                    if (selectedFiles.isNotEmpty()) {
                        selectedFiles.clear()
                    }
                    if (allFiles.isNotEmpty()) {
                        allFiles = emptyList()
                    }
                    onBackPressedDispatcher?.onBackPressed()
                }
            }
            is BucketScreenViewModel.UiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is BucketScreenViewModel.UiState.Bucket -> {
                val files = uiState.files
                bucketHidden = uiState.isBucketHidden
                allFiles = files
                SketchesMediaGrid(
                    files = files,
                    selectedFiles = selectedFiles,
                    onItemClick = onImageClick,
                    modifier = Modifier
                        .matchParentSize()
                        .nestedScroll(mediaGridScrollConnection),
                    state = mediaGridState,
                    overlayTop = true,
                    overlayBottom = false,
                )
            }
            is BucketScreenViewModel.UiState.Error -> {
                SketchesErrorMessage(
                    thrown = uiState.thrown,
                    modifier = Modifier.matchParentSize(),
                )
                LaunchedEffect(Unit) {
                    mediaGridScrollConnection.reset()
                    if (selectedFiles.isNotEmpty()) {
                        selectedFiles.clear()
                    }
                    if (allFiles.isNotEmpty()) {
                        allFiles = emptyList()
                    }
                }
            }
        }
        val appBarVisible by remember {
            derivedStateOf(structuralEqualityPolicy()) {
                !mediaGridState.canScrollForward && !mediaGridState.canScrollBackward ||
                    mediaGridScrollConnection.neverScrolled ||
                    mediaGridScrollConnection.lastScrolledBackward ||
                    selectedFiles.isNotEmpty()
            }
        }
        SketchesTopAppBar(
            text = if (selectedFiles.isNotEmpty()) {
                stringResource(
                    R.string.selected_count,
                    selectedFiles.size,
                )
            } else {
                bucketName
            },
            visible = appBarVisible,
        ) {
            SketchesActionButton(
                icon = painterResource(
                    if (bucketHidden) {
                        R.drawable.ic_bucket_show
                    } else {
                        R.drawable.ic_bucket_hide
                    },
                ),
                hint = stringResource(
                    if (bucketHidden) {
                        R.string.show_bucket
                    } else {
                        R.string.hide_bucket
                    },
                ),
                onClick = if (bucketHidden) {
                    onShowBucket
                } else {
                    onHideBucket
                },
            )
            val selectionMode by remember {
                derivedStateOf(structuralEqualityPolicy()) {
                    selectedFiles.isNotEmpty()
                }
            }
            if (selectionMode) {
                val allFilesSelected by remember {
                    derivedStateOf(structuralEqualityPolicy()) {
                        selectedFiles.size >= allFiles.size
                    }
                }
                SketchesActionButton(
                    icon = painterResource(
                        if (allFilesSelected) {
                            R.drawable.ic_select_none
                        } else {
                            R.drawable.ic_select_all
                        },
                    ),
                    hint = stringResource(
                        if (allFilesSelected) {
                            R.string.select_none
                        } else {
                            R.string.select_all
                        },
                    ),
                    onClick = {
                        coroutineScope.launch {
                            if (allFilesSelected) {
                                selectedFiles.clear()
                            } else {
                                selectedFiles.addAll(allFiles.map { file -> file.id })
                            }
                        }
                    },
                )
                SketchesActionButton(
                    icon = painterResource(R.drawable.ic_delete),
                    hint = stringResource(R.string.delete_selected),
                    onClick = {
                        deleteDialogVisible = true
                    },
                )
                val shareTitle by rememberUpdatedState(stringResource(R.string.share_selected))
                SketchesActionButton(
                    icon = painterResource(R.drawable.ic_share),
                    hint = shareTitle,
                    onClick = {
                        coroutineScope.launch {
                            allFiles.prepareForSharing(
                                filterIds = selectedFiles.toSet(),
                                mediaSizeLimit = MediaBatchState.BatchSize,
                            ) { media, mimeType ->
                                mediaBatchState.start(
                                    media = media,
                                    payload = BatchAction.Share(
                                        chooserTitle = shareTitle,
                                        mimeType = mimeType,
                                    ),
                                )
                            }
                        }
                    },
                )
            }
        }
        val navBarInsets = WindowInsets.navigationBars
        val bottomNavBarHeight = navBarInsets.asPaddingValues().calculateBottomPadding()
        if (bottomNavBarHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(bottomNavBarHeight)
                    .background(
                        brush = rememberBottomToTopBackgroundGradientBrush(colorScheme),
                        shape = RectangleShape,
                    ),
            )
        }
        val scrollToStartButtonVisible by remember {
            derivedStateOf(structuralEqualityPolicy()) {
                mediaGridScrollConnection.lastScrolledBackward &&
                    mediaGridState.canScrollBackward ||
                    selectedFiles.isNotEmpty()
            }
        }
        val scrollToStartButtonAlpha by animateFloatAsState(
            targetValue = if (scrollToStartButtonVisible) 1F else 0F,
            animationSpec = DefaultAlphaAnimationSpec,
        )
        if (scrollToStartButtonAlpha > 0F) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = scrollToStartButtonAlpha
                    }
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        navBarInsets.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.End,
                        ),
                    )
                    .padding(24.dp)
                    .dropShadow(
                        shape = CircleShape,
                        shadow = Shadow(
                            radius = LocalDimens.current.shadowBlurRadius,
                            color = colorScheme.scrim.withLowTransparency(),
                        ),
                    ),
            ) {
                FloatingActionButton(
                    modifier = Modifier.align(Alignment.Center),
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                    ),
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    onClick = {
                        coroutineScope.launch {
                            if (allFiles.isNotEmpty()) {
                                mediaGridScrollConnection.reset()
                                mediaGridState.animateScrollToItem(index = 0)
                            }
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_scroll_to_start),
                        contentDescription = stringResource(R.string.scroll_to_start),
                        tint = colorScheme.onPrimary,
                    )
                }
            }
        }
        if (deleteDialogVisible) {
            SketchesDeleteImagesConfirmationDialog(
                count = selectedFiles.size,
                onDelete = {
                    deleteDialogVisible = false
                    coroutineScope.launch {
                        val snapshot = selectedFiles.toSet()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            mediaBatchState.start(
                                media = allFiles.toMediaDescriptorList(snapshot),
                                payload = BatchAction.Delete,
                            )
                        } else {
                            selectedFiles.clear()
                            onDeleteMedia(allFiles.toUriList(snapshot))
                        }
                    }
                },
                onDismiss = {
                    deleteDialogVisible = false
                },
            )
        }
    }
}

private const val ShareAction: String =
    "com.github.yuriybudiyev.sketches.feature.bucket.ui.ShareAction"

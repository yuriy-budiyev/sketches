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
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.component1
import androidx.activity.result.component2
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.data.utils.filterByIds
import com.github.yuriybudiyev.sketches.core.navigation.LocalNavResultStore
import com.github.yuriybudiyev.sketches.core.platform.bars.LocalSystemBarsController
import com.github.yuriybudiyev.sketches.core.platform.content.launchDeleteMediaRequest
import com.github.yuriybudiyev.sketches.core.platform.share.LocalShareManager
import com.github.yuriybudiyev.sketches.core.platform.share.toShareInfo
import com.github.yuriybudiyev.sketches.core.saver.SnapshotStateSetSaver
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAppBarActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesDeleteConfirmationDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesMediaGrid
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesMediaGridContentType
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.components.rememberSketchesLazyGridState
import com.github.yuriybudiyev.sketches.core.ui.scroll.scrollToItemClosestEdge
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageScreenNavResult
import kotlinx.coroutines.launch

@Composable
fun BucketRoute(
    viewModel: BucketScreenViewModel,
    onImageClick: (index: Int, file: MediaStoreFile) -> Unit,
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
        }
    )
}

@Composable
fun BucketScreen(
    bucketName: String,
    uiState: BucketScreenViewModel.UiState,
    onImageClick: (index: Int, file: MediaStoreFile) -> Unit,
    onDeleteMedia: (files: Collection<MediaStoreFile>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val contextUpdated by rememberUpdatedState(LocalContext.current)
    val shareManagerUpdated by rememberUpdatedState(LocalShareManager.current)
    val onDeleteMediaUpdated by rememberUpdatedState(onDeleteMedia)
    var allFiles by remember { mutableStateOf<Collection<MediaStoreFile>>(emptyList()) }
    val selectedFiles =
        rememberSaveable(saver = SnapshotStateSetSaver()) { SnapshotStateSet<Long>() }
    var deleteDialogVisible by rememberSaveable { mutableStateOf(false) }
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { (resultCode, _) ->
            if (resultCode == Activity.RESULT_OK) {
                coroutineScope.launch {
                    selectedFiles.clear()
                }
            }
        },
    )
    DisposableEffect(Unit) {
        shareManagerUpdated.registerOnSharedListener(ShareAction) {
            coroutineScope.launch {
                selectedFiles.clear()
            }
        }
        onDispose {
            shareManagerUpdated.unregisterOnSharedListener(ShareAction)
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
    val mediaGridState = rememberSketchesLazyGridState()
    val scrollToStartButtonVisible by remember {
        derivedStateOf {
            with(mediaGridState) {
                lastScrolledBackward && !(firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0)
            }
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
                mediaGridState.scrollToItemClosestEdge(
                    index = result.fileIndex,
                    itemType = SketchesMediaGridContentType.MediaStoreFile,
                    animate = false,
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        when (uiState) {
            is BucketScreenViewModel.UiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(R.string.no_images_found),
                    modifier = Modifier.matchParentSize(),
                )
                SideEffect {
                    if (selectedFiles.isNotEmpty()) {
                        selectedFiles.clear()
                    }
                    if (allFiles.isNotEmpty()) {
                        allFiles = emptyList()
                    }
                }
            }
            is BucketScreenViewModel.UiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is BucketScreenViewModel.UiState.Bucket -> {
                val files = uiState.files
                allFiles = files
                SketchesMediaGrid(
                    files = files,
                    selectedFiles = selectedFiles,
                    onItemClick = onImageClick,
                    modifier = Modifier.matchParentSize(),
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
                SideEffect {
                    if (selectedFiles.isNotEmpty()) {
                        selectedFiles.clear()
                    }
                    if (allFiles.isNotEmpty()) {
                        allFiles = emptyList()
                    }
                }
            }
        }
        SketchesTopAppBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            text = if (selectedFiles.isNotEmpty()) {
                stringResource(
                    R.string.selected_count,
                    selectedFiles.size,
                )
            } else {
                bucketName
            },
            backgroundColor = MaterialTheme.colorScheme.background
                .copy(alpha = SketchesColors.UiAlphaLowTransparency),
        ) {
            if (selectedFiles.isNotEmpty()) {
                if (selectedFiles.size >= allFiles.size) {
                    SketchesAppBarActionButton(
                        iconRes = R.drawable.ic_select_none,
                        description = stringResource(R.string.select_none),
                        onClick = {
                            coroutineScope.launch {
                                selectedFiles.clear()
                            }
                        },
                    )
                } else {
                    SketchesAppBarActionButton(
                        iconRes = R.drawable.ic_select_all,
                        description = stringResource(R.string.select_all),
                        onClick = {
                            coroutineScope.launch {
                                selectedFiles.addAll(allFiles.map { file -> file.id })
                            }
                        },
                    )
                }
                SketchesAppBarActionButton(
                    iconRes = R.drawable.ic_delete,
                    description = stringResource(R.string.delete_selected),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            coroutineScope.launch {
                                deleteRequestLauncher.launchDeleteMediaRequest(
                                    contextUpdated,
                                    allFiles
                                        .filterByIds(selectedFiles.toSet())
                                        .map { file -> file.uri },
                                )
                            }
                        } else {
                            deleteDialogVisible = true
                        }
                    },
                )
                val shareDescription = stringResource(R.string.share_selected)
                SketchesAppBarActionButton(
                    iconRes = R.drawable.ic_share,
                    description = shareDescription,
                    onClick = {
                        coroutineScope.launch {
                            val shareInfo = allFiles
                                .filterByIds(selectedFiles.toSet())
                                .toShareInfo()
                            shareManagerUpdated.startChooserActivity(
                                uris = shareInfo.uris,
                                mimeType = shareInfo.mimeType,
                                chooserTitle = shareDescription,
                                listenerAction = ShareAction,
                            )
                        }
                    },
                )
            }
        }
        var contentInsets = WindowInsets.navigationBars
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            contentInsets = contentInsets
                .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
        }
        AnimatedVisibility(
            visible = scrollToStartButtonVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(contentInsets.asPaddingValues())
                .padding(16.dp),
        ) {
            FloatingActionButton(
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                ),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = {
                    coroutineScope.launch {
                        mediaGridState.animateScrollToItem(0)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_scroll_to_start),
                    contentDescription = stringResource(R.string.scroll_to_start),
                )
            }
        }
        if (deleteDialogVisible) {
            SketchesDeleteConfirmationDialog(
                onDelete = {
                    deleteDialogVisible = false
                    onDeleteMediaUpdated(allFiles.filterByIds(selectedFiles.toSet()))
                    coroutineScope.launch {
                        selectedFiles.clear()
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

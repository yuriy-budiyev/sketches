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

package com.github.yuriybudiyev.sketches.feature.images.ui

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.component1
import androidx.activity.result.component2
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaFile
import com.github.yuriybudiyev.sketches.core.navigation.LocalNavResultStore
import com.github.yuriybudiyev.sketches.core.navigation.LocalRootNavMenuController
import com.github.yuriybudiyev.sketches.core.platform.bars.LocalSystemBarsController
import com.github.yuriybudiyev.sketches.core.platform.content.launchDeleteMediaRequest
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.OnRequestMediaAccess
import com.github.yuriybudiyev.sketches.core.platform.share.LocalShareManager
import com.github.yuriybudiyev.sketches.core.saveable.rememberSaveableSnapshotStateSet
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesDeleteImagesConfirmationDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.components.media.SketchesGroupingMediaGrid
import com.github.yuriybudiyev.sketches.core.ui.components.media.SketchesMediaGridContentType
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.BatchAction
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.MediaBatchState
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.rememberMediaBatchState
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.toMediaDescriptorList
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.toUriList
import com.github.yuriybudiyev.sketches.core.ui.components.media.calculateMediaIndexWithGroups
import com.github.yuriybudiyev.sketches.core.ui.components.media.share.prepareForSharing
import com.github.yuriybudiyev.sketches.core.ui.utils.findFirstVisibleItemIndex
import com.github.yuriybudiyev.sketches.core.ui.utils.scrollToItem
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageScreenNavResult
import com.github.yuriybudiyev.sketches.feature.images.navigation.ImagesNavRoute
import kotlinx.coroutines.launch

@Composable
fun ImagesRoute(
    viewModel: ImagesScreenViewModel,
    onImageClick: (index: Int, file: MediaFile) -> Unit,
    onRequestMediaAccess: OnRequestMediaAccess,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateMediaAccess()
    }
    ImagesScreen(
        uiState = uiState,
        onRequestMediaAccess = onRequestMediaAccess,
        onImageClick = onImageClick,
        onDeleteMedia = { files ->
            viewModel.deleteMedia(files)
        },
    )
}

@Composable
fun ImagesScreen(
    uiState: ImagesScreenViewModel.UiState,
    onRequestMediaAccess: OnRequestMediaAccess,
    onImageClick: (index: Int, file: MediaFile) -> Unit,
    onDeleteMedia: (files: Collection<Uri>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context by rememberUpdatedState(LocalContext.current)
    val shareManager by rememberUpdatedState(LocalShareManager.current)
    val onDeleteMedia by rememberUpdatedState(onDeleteMedia)
    var allFiles by remember { mutableStateOf<Collection<MediaFile>>(emptyList()) }
    val selectedFiles = rememberSaveableSnapshotStateSet<Long>()
    var deleteDialogVisible by rememberSaveable { mutableStateOf(false) }
    val mediaGridState = rememberLazyGridState()
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
            coroutineScope.launch {
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
        shareManager.registerOnSharedListener(ShareAction) {
            coroutineScope.launch {
                selectedFiles.removeAll(currentBatch)
                mediaBatchState.reset()
                if (selectedFiles.isNotEmpty()) {
                    mediaGridState.scrollToItem(
                        files = allFiles,
                        snapToClosestEdge = false,
                    ) { _, file ->
                        selectedFiles.contains(file.id)
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
                    files = allFiles,
                    snapToClosestEdge = true,
                ) { index, _ ->
                    index == result.fileIndex
                }
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
    val rootNavMenuController = LocalRootNavMenuController.current
    LaunchedEffect(rootNavMenuController) {
        snapshotFlow { selectedFiles.toSet().isNotEmpty() }.collect { hasSelectedFiles ->
            if (hasSelectedFiles) {
                rootNavMenuController.hideNavMenu()
            } else {
                rootNavMenuController.showNavMenu()
            }
        }
    }
    DisposableEffect(rootNavMenuController) {
        rootNavMenuController.setOnClickListener(ImagesNavRoute) {
            coroutineScope.launch {
                if (allFiles.isNotEmpty()) {
                    mediaGridState.animateScrollToItem(index = 0)
                }
            }
        }
        onDispose {
            rootNavMenuController.clearOnClickListener(ImagesNavRoute)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        when (uiState) {
            is ImagesScreenViewModel.UiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(R.string.no_images_found),
                    modifier = Modifier.matchParentSize(),
                )
                LaunchedEffect(Unit) {
                    if (selectedFiles.isNotEmpty()) {
                        selectedFiles.clear()
                    }
                    if (allFiles.isNotEmpty()) {
                        allFiles = emptyList()
                    }
                }
            }
            is ImagesScreenViewModel.UiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is ImagesScreenViewModel.UiState.Images -> {
                allFiles = uiState.files
                SketchesGroupingMediaGrid(
                    items = uiState.groups,
                    selectedFiles = selectedFiles,
                    onItemClick = onImageClick,
                    modifier = Modifier.matchParentSize(),
                    state = mediaGridState,
                    overlayTop = true,
                    overlayBottom = true,
                )
            }
            is ImagesScreenViewModel.UiState.Error -> {
                SketchesErrorMessage(
                    thrown = uiState.thrown,
                    modifier = Modifier.matchParentSize(),
                )
                LaunchedEffect(Unit) {
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
            derivedStateOf {
                mediaGridState.findFirstVisibleItemIndex() <= 0 || selectedFiles.isNotEmpty()
            }
        }
        SketchesTopAppBar(
            text = if (selectedFiles.isNotEmpty()) {
                stringResource(
                    R.string.selected_count,
                    selectedFiles.size,
                )
            } else {
                stringResource(ImagesNavRoute.titleRes)
            },
            visible = appBarVisible,
        ) {
            if (onRequestMediaAccess.isEnabled) {
                SketchesActionButton(
                    icon = painterResource(R.drawable.ic_media_permission),
                    hint = stringResource(R.string.request_media_access),
                    onClick = {
                        onRequestMediaAccess()
                    },
                )
            }
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

private suspend inline fun LazyGridState.scrollToItem(
    files: Collection<MediaFile>,
    snapToClosestEdge: Boolean,
    predicate: (index: Int, file: MediaFile) -> Boolean,
) {
    if (files.isEmpty()) {
        return
    }
    val itemIndex = calculateMediaIndexWithGroups(
        files = files,
        predicate = predicate,
    )
    if (itemIndex != -1) {
        scrollToItem(
            index = itemIndex,
            itemType = SketchesMediaGridContentType.MediaStoreFile,
            animate = false,
            snapToClosestEdge = snapToClosestEdge,
            onlyIfItemAtIndexIsNotVisible = true,
        )
    }
}

private const val ShareAction: String =
    "com.github.yuriybudiyev.sketches.feature.images.ui.ShareAction"

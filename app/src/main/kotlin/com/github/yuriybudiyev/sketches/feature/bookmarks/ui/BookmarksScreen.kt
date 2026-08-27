/*
 * MIT License
 *
 * Copyright (c) 2026 Yuriy Budiyev
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

package com.github.yuriybudiyev.sketches.feature.bookmarks.ui

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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.github.yuriybudiyev.sketches.core.platform.share.LocalShareManager
import com.github.yuriybudiyev.sketches.core.saveable.rememberSaveableSnapshotStateSet
import com.github.yuriybudiyev.sketches.core.ui.colors.withLowTransparency
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAppBarActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesDeleteBookmarksConfirmationDialog
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
import com.github.yuriybudiyev.sketches.core.ui.scroll.scrollToItem
import com.github.yuriybudiyev.sketches.feature.bookmarks.navigation.BookmarksNavRoute
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageScreenNavResult
import kotlinx.coroutines.launch

@Composable
fun BookmarksRoute(
    viewModel: BookmarksScreenViewModel,
    onImageClick: (index: Int, file: MediaFile) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateMediaAccess()
    }
    BookmarksScreen(
        uiState = uiState,
        onImageClick = onImageClick,
        onDeleteMedia = { files ->
            viewModel.deleteMedia(files)
        },
        onDeleteBookmarks = { mediaIds ->
            viewModel.deleteBookmarks(mediaIds)
        },
    )
}

@Composable
private fun BookmarksScreen(
    uiState: BookmarksScreenViewModel.UiState,
    onImageClick: (index: Int, file: MediaFile) -> Unit,
    onDeleteMedia: (files: Collection<Uri>) -> Unit,
    onDeleteBookmarks: (mediaIds: Collection<Long>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context by rememberUpdatedState(LocalContext.current)
    val shareManager by rememberUpdatedState(LocalShareManager.current)
    val onDeleteMediaUpdated by rememberUpdatedState(onDeleteMedia)
    val onDeleteBookmarksUpdated by rememberUpdatedState(onDeleteBookmarks)
    var allFiles by remember { mutableStateOf<Collection<MediaFile>>(emptyList()) }
    val selectedFiles = rememberSaveableSnapshotStateSet<Long>()
    var deleteFilesDialogVisible by rememberSaveable { mutableStateOf(false) }
    var deleteBookmarksDialogVisible by rememberSaveable { mutableStateOf(false) }
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
            deleteFilesDialogVisible = false
            deleteBookmarksDialogVisible = false
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
    val rootNavMenuController = LocalRootNavMenuController.current
    LaunchedEffect(rootNavMenuController) {
        snapshotFlow { selectedFiles.toSet().isNotEmpty() }.collect { hasSelectedFiles ->
            if (hasSelectedFiles) {
                rootNavMenuController.hide()
            } else {
                rootNavMenuController.show()
            }
        }
    }
    DisposableEffect(rootNavMenuController) {
        rootNavMenuController.setOnClickListener(BookmarksNavRoute) {
            coroutineScope.launch {
                if (allFiles.isNotEmpty()) {
                    mediaGridState.animateScrollToItem(index = 0)
                }
            }
        }
        onDispose {
            rootNavMenuController.clearOnClickListener(BookmarksNavRoute)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        when (uiState) {
            is BookmarksScreenViewModel.UiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(R.string.no_bookmarks_found),
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
            is BookmarksScreenViewModel.UiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is BookmarksScreenViewModel.UiState.Bookmarks -> {
                allFiles = uiState.files
                SketchesMediaGrid(
                    files = uiState.files,
                    selectedFiles = selectedFiles,
                    onItemClick = onImageClick,
                    modifier = Modifier.matchParentSize(),
                    state = mediaGridState,
                    overlayTop = true,
                    overlayBottom = true,
                )
            }
            is BookmarksScreenViewModel.UiState.Error -> {
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
                stringResource(BookmarksNavRoute.titleRes)
            },
            backgroundColor = MaterialTheme.colorScheme.background.withLowTransparency(),
        ) {
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
                SketchesAppBarActionButton(
                    iconRes = if (allFilesSelected) {
                        R.drawable.ic_select_none
                    } else {
                        R.drawable.ic_select_all
                    },
                    description = stringResource(
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
                SketchesAppBarActionButton(
                    iconRes = R.drawable.ic_bookmark_delete,
                    description = stringResource(R.string.delete_bookmarks),
                    onClick = {
                        deleteBookmarksDialogVisible = true
                    },
                )
                SketchesAppBarActionButton(
                    iconRes = R.drawable.ic_delete,
                    description = stringResource(R.string.delete_selected),
                    onClick = {
                        deleteFilesDialogVisible = true
                    },
                )
                val shareTitle by rememberUpdatedState(stringResource(R.string.share_selected))
                SketchesAppBarActionButton(
                    iconRes = R.drawable.ic_share,
                    description = shareTitle,
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
        if (deleteFilesDialogVisible) {
            SketchesDeleteImagesConfirmationDialog(
                count = selectedFiles.size,
                onDelete = {
                    deleteFilesDialogVisible = false
                    coroutineScope.launch {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            mediaBatchState.start(
                                media = allFiles.toMediaDescriptorList(selectedFiles.toSet()),
                                payload = BatchAction.Delete,
                            )
                        } else {
                            onDeleteMediaUpdated(allFiles.toUriList(selectedFiles.toSet()))
                            selectedFiles.clear()
                        }
                    }
                },
                onDismiss = {
                    deleteFilesDialogVisible = false
                },
            )
        }
        if (deleteBookmarksDialogVisible) {
            SketchesDeleteBookmarksConfirmationDialog(
                count = selectedFiles.size,
                onDelete = {
                    deleteBookmarksDialogVisible = false
                    coroutineScope.launch {
                        onDeleteBookmarksUpdated(selectedFiles.toSet())
                        selectedFiles.clear()
                    }
                },
                onDismiss = {
                    deleteBookmarksDialogVisible = false
                },
            )
        }
    }
}

private const val ShareAction: String =
    "com.github.yuriybudiyev.sketches.feature.bookmarks.ui.ShareAction"

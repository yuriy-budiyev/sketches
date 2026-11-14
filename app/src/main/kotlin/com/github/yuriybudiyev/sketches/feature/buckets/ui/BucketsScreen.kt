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

package com.github.yuriybudiyev.sketches.feature.buckets.ui

import kotlin.math.min
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaBucket
import com.github.yuriybudiyev.sketches.core.navigation.LocalRootNavMenuController
import com.github.yuriybudiyev.sketches.core.platform.content.launchDeleteMediaRequestOrThrow
import com.github.yuriybudiyev.sketches.core.saveable.rememberSaveableSnapshotStateList
import com.github.yuriybudiyev.sketches.core.saveable.rememberSaveableSnapshotStateSet
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesDeleteImagesConfirmationDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.appbar.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.components.appbar.actions.SelectAction
import com.github.yuriybudiyev.sketches.core.ui.components.appbar.actions.SimpleAction
import com.github.yuriybudiyev.sketches.core.ui.components.fastAnimateScrollToStart
import com.github.yuriybudiyev.sketches.core.ui.components.media.SketchesMediaBucketsGrid
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.BatchAction
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.MediaBatchHandler
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.MediaDescriptor
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.rememberMediaBatchState
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.toMediaDescriptorList
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.toUriList
import com.github.yuriybudiyev.sketches.core.ui.utils.rememberLastScrollDirectionScrollConnection
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsNavRoute

@Composable
fun BucketsRoute(
    viewModel: BucketsScreenViewModel,
    onBucketClick: (index: Int, bucket: MediaBucket) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateMediaAccess()
    }
    BucketsScreen(
        uiState = uiState,
        onBucketClick = onBucketClick,
        onDeleteBuckets = { buckets ->
            viewModel.startDeletingBuckets(buckets)
        },
        onDeleteMedia = { uris ->
            viewModel.deleteMedia(uris)
        },
    )
}

@Composable
fun BucketsScreen(
    uiState: BucketsScreenViewModel.UiState,
    onBucketClick: (index: Int, bucket: MediaBucket) -> Unit,
    onDeleteBuckets: (buckets: Collection<MediaBucket>) -> Unit,
    onDeleteMedia: (uris: Collection<Uri>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val onDeleteBuckets by rememberUpdatedState(onDeleteBuckets)
    val onDeleteMedia by rememberUpdatedState(onDeleteMedia)
    var allBuckets by remember { mutableStateOf<List<MediaBucket>>(emptyList()) }
    val selectedBuckets = rememberSaveableSnapshotStateSet<Long>()
    val deleteDialogMedia = rememberSaveableSnapshotStateList<MediaDescriptor>()
    val mediaBatchState = rememberMediaBatchState()
    val mediaGridState = rememberLazyGridState()
    val mediaGridScrollConnection = rememberLastScrollDirectionScrollConnection(mediaGridState)
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { [resultCode, _] ->
            coroutineScope.launch {
                if (resultCode == Activity.RESULT_OK) {
                    mediaBatchState.proceed()
                } else {
                    mediaBatchState.reset()
                }
            }
        },
    )
    MediaBatchHandler(
        state = mediaBatchState,
        onDelete = { uris ->
            deleteRequestLauncher.launchDeleteMediaRequestOrThrow(context, uris)
        },
        onFinish = {
            selectedBuckets.clear()
        },
    )
    LaunchedEffect(Unit) {
        if (selectedBuckets.isEmpty()) {
            deleteDialogMedia.clear()
        }
    }
    BackHandler(selectedBuckets.isNotEmpty()) {
        coroutineScope.launch {
            selectedBuckets.clear()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(
        uiState,
        lifecycleOwner,
    ) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (uiState is BucketsScreenViewModel.UiState.Buckets) {
                when (val action = uiState.action.consume()) {
                    is BucketsScreenViewModel.UiState.Buckets.Action.Delete -> {
                        if (deleteDialogMedia.isNotEmpty()) {
                            deleteDialogMedia.clear()
                        }
                        deleteDialogMedia.addAll(action.files.toMediaDescriptorList())
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }
        }
    }
    val rootNavMenuController = LocalRootNavMenuController.current
    LaunchedEffect(rootNavMenuController) {
        snapshotFlow { selectedBuckets.toSet().isNotEmpty() }.collect { hasSelectedBuckets ->
            if (hasSelectedBuckets) {
                rootNavMenuController.hideNavMenu()
            } else {
                rootNavMenuController.showNavMenu()
            }
        }
    }
    DisposableEffect(rootNavMenuController) {
        rootNavMenuController.setOnClickListener(BucketsNavRoute) {
            coroutineScope.launch {
                if (allBuckets.isNotEmpty()) {
                    mediaGridScrollConnection.reset()
                    mediaGridState.fastAnimateScrollToStart()
                }
            }
        }
        onDispose {
            rootNavMenuController.clearOnClickListener(BucketsNavRoute)
        }
    }
    val appBarVisible by remember {
        computedStateOf {
            !mediaGridState.canScrollForward && !mediaGridState.canScrollBackward ||
                mediaGridScrollConnection.neverScrolled ||
                mediaGridScrollConnection.lastScrolledBackward ||
                selectedBuckets.isNotEmpty()
        }
    }
    val inSelectionMode by remember { computedStateOf { selectedBuckets.isNotEmpty() } }
    val allBucketsSelected by remember { computedStateOf { selectedBuckets.size >= allBuckets.size } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        when (uiState) {
            is BucketsScreenViewModel.UiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(R.string.no_buckets_found),
                    modifier = Modifier.matchParentSize(),
                )
                LaunchedEffect(Unit) {
                    mediaGridScrollConnection.reset()
                    if (selectedBuckets.isNotEmpty()) {
                        selectedBuckets.clear()
                    }
                    if (deleteDialogMedia.isNotEmpty()) {
                        deleteDialogMedia.clear()
                    }
                    if (allBuckets.isNotEmpty()) {
                        allBuckets = emptyList()
                    }
                }
            }
            is BucketsScreenViewModel.UiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is BucketsScreenViewModel.UiState.Buckets -> {
                val buckets = uiState.buckets
                allBuckets = buckets
                SketchesMediaBucketsGrid(
                    buckets = buckets,
                    selectedBuckets = selectedBuckets,
                    onItemClick = onBucketClick,
                    modifier = Modifier
                        .matchParentSize()
                        .nestedScroll(mediaGridScrollConnection),
                    state = mediaGridState,
                    overlayTop = true,
                    overlayBottom = true,
                )
            }
            is BucketsScreenViewModel.UiState.Error -> {
                SketchesErrorMessage(
                    thrown = uiState.thrown,
                    modifier = Modifier.matchParentSize(),
                )
                LaunchedEffect(Unit) {
                    mediaGridScrollConnection.reset()
                    if (selectedBuckets.isNotEmpty()) {
                        selectedBuckets.clear()
                    }
                    if (deleteDialogMedia.isNotEmpty()) {
                        deleteDialogMedia.clear()
                    }
                    if (allBuckets.isNotEmpty()) {
                        allBuckets = emptyList()
                    }
                }
            }
        }
        SketchesTopAppBar(
            text = if (selectedBuckets.isNotEmpty()) {
                stringResource(
                    R.string.selected_count,
                    selectedBuckets.size,
                )
            } else {
                stringResource(BucketsNavRoute.titleRes)
            },
            visible = appBarVisible,
        ) {
            if (inSelectionMode) {
                SelectAction(
                    selected = allBucketsSelected,
                    onSelectAll = {
                        coroutineScope.launch {
                            selectedBuckets.addAll(allBuckets.map { bucket -> bucket.id })
                        }
                    },
                    onSelectNone = {
                        coroutineScope.launch {
                            selectedBuckets.clear()
                        }
                    },
                )
                SimpleAction(
                    iconRes = R.drawable.ic_delete,
                    hintRes = R.string.delete_selected,
                    onClick = {
                        coroutineScope.launch {
                            onDeleteBuckets(allBuckets.filterByIds(selectedBuckets.toSet()))
                        }
                    },
                )
            }
        }
        if (deleteDialogMedia.isNotEmpty()) {
            SketchesDeleteImagesConfirmationDialog(
                count = deleteDialogMedia.size,
                onDelete = {
                    coroutineScope.launch {
                        val snapshot = deleteDialogMedia.toList()
                        deleteDialogMedia.clear()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            mediaBatchState.start(
                                media = snapshot,
                                payload = BatchAction.Delete,
                            )
                        } else {
                            selectedBuckets.clear()
                            onDeleteMedia(snapshot.toUriList())
                        }
                    }
                },
                onDismiss = {
                    coroutineScope.launch {
                        deleteDialogMedia.clear()
                    }
                },
            )
        }
    }
}

private fun Collection<MediaBucket>.filterByIds(ids: Set<Long>): List<MediaBucket> {
    val size = min(this.size, ids.size)
    if (size == 0) {
        return emptyList()
    }
    val filtered = ArrayList<MediaBucket>(size)
    for (bucket in this) {
        if (ids.contains(bucket.id)) {
            filtered.add(bucket)
        }
        if (filtered.size == size) {
            break
        }
    }
    return filtered
}

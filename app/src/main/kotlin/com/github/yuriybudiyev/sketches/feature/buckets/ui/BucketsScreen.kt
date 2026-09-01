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

import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.component1
import androidx.activity.result.component2
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaBucket
import com.github.yuriybudiyev.sketches.core.navigation.LocalRootNavMenuController
import com.github.yuriybudiyev.sketches.core.platform.content.launchDeleteMediaRequest
import com.github.yuriybudiyev.sketches.core.saveable.rememberSaveableSnapshotStateList
import com.github.yuriybudiyev.sketches.core.saveable.rememberSaveableSnapshotStateSet
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesDeleteImagesConfirmationDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLazyGrid
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.components.media.SketchesThumbnailAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.BatchAction
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.MediaBatchState
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.MediaDescriptor
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.rememberMediaBatchState
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.toMediaDescriptorList
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.toUriList
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens
import com.github.yuriybudiyev.sketches.core.ui.theme.withHighTransparency
import com.github.yuriybudiyev.sketches.core.ui.theme.withLowTransparency
import com.github.yuriybudiyev.sketches.core.ui.theme.withMediumTransparency
import com.github.yuriybudiyev.sketches.core.ui.utils.rememberLastScrolledScrollConnection
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsNavRoute
import kotlinx.coroutines.launch

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
    val context by rememberUpdatedState(LocalContext.current)
    val onDeleteBuckets by rememberUpdatedState(onDeleteBuckets)
    val onDeleteMedia by rememberUpdatedState(onDeleteMedia)
    var allBuckets by remember { mutableStateOf<List<MediaBucket>>(emptyList()) }
    val selectedBuckets = rememberSaveableSnapshotStateSet<Long>()
    val deleteDialogMedia = rememberSaveableSnapshotStateList<MediaDescriptor>()
    val mediaBatchState = rememberMediaBatchState()
    val bucketsGridState = rememberLazyGridState()
    val bucketsGridScrollConnection = rememberLastScrolledScrollConnection(bucketsGridState)
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { (resultCode, _) ->
            coroutineScope.launch {
                if (resultCode == Activity.RESULT_OK) {
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
                    if (action.payload is BatchAction.Delete) {
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
                is MediaBatchState.Action.Finish -> {
                    selectedBuckets.clear()
                }
                is MediaBatchState.Action.Reset -> {
                    // Do nothing
                }
            }
        }
    }
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
                    bucketsGridState.animateScrollToItem(index = 0)
                }
            }
        }
        onDispose {
            rootNavMenuController.clearOnClickListener(BucketsNavRoute)
        }
    }
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
                    bucketsGridScrollConnection.reset()
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
                BucketsMediaGrid(
                    state = bucketsGridState,
                    buckets = buckets,
                    selectedBuckets = selectedBuckets,
                    onBucketClick = onBucketClick,
                    modifier = Modifier
                        .matchParentSize()
                        .nestedScroll(bucketsGridScrollConnection),
                )
            }
            is BucketsScreenViewModel.UiState.Error -> {
                SketchesErrorMessage(
                    thrown = uiState.thrown,
                    modifier = Modifier.matchParentSize(),
                )
                LaunchedEffect(Unit) {
                    bucketsGridScrollConnection.reset()
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
        val appBarVisible by remember {
            derivedStateOf(structuralEqualityPolicy()) {
                with(bucketsGridScrollConnection) {
                    neverScrolled || lastScrolledBackward || selectedBuckets.isNotEmpty()
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
            val selectionMode by remember {
                derivedStateOf(structuralEqualityPolicy()) {
                    selectedBuckets.isNotEmpty()
                }
            }
            if (selectionMode) {
                val allFilesSelected by remember {
                    derivedStateOf(structuralEqualityPolicy()) {
                        selectedBuckets.size >= allBuckets.size
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
                                selectedBuckets.clear()
                            } else {
                                selectedBuckets.addAll(allBuckets.map { bucket -> bucket.id })
                            }
                        }
                    },
                )
                SketchesActionButton(
                    icon = painterResource(R.drawable.ic_delete),
                    hint = stringResource(R.string.delete_selected),
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

@Composable
private fun BucketsMediaGrid(
    state: LazyGridState,
    buckets: List<MediaBucket>,
    selectedBuckets: SnapshotStateSet<Long>,
    onBucketClick: (index: Int, bucket: MediaBucket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buckets by rememberUpdatedState(buckets)
    val selectedBuckets by rememberUpdatedState(selectedBuckets)
    val onBucketClick by rememberUpdatedState(onBucketClick)
    val colorScheme = MaterialTheme.colorScheme
    val dimens = LocalDimens.current
    SketchesLazyGrid(
        modifier = modifier,
        state = state,
        overlayTop = true,
        overlayBottom = true,
    ) {
        items(
            count = buckets.size,
            key = { index -> buckets[index].id },
            contentType = { null },
        ) { index ->
            val bucket by rememberUpdatedState(buckets[index])
            val bucketSelected by remember {
                derivedStateOf(structuralEqualityPolicy()) {
                    selectedBuckets.contains(bucket.id)
                }
            }
            Column(
                modifier = Modifier
                    .animateItem()
                    .clipToBounds()
                    .combinedClickable(
                        onLongClick = {
                            if (selectedBuckets.isEmpty()) {
                                selectedBuckets.add(bucket.id)
                            }
                        },
                        onClick = {
                            if (selectedBuckets.isNotEmpty()) {
                                if (bucketSelected) {
                                    selectedBuckets.remove(bucket.id)
                                } else {
                                    selectedBuckets.add(bucket.id)
                                }
                            } else {
                                onBucketClick(
                                    index,
                                    bucket,
                                )
                            }
                        },
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio = 1F)
                        .border(
                            width = dimens.mediaItemBorderThickness,
                            color = if (bucketSelected) {
                                colorScheme.onBackground.withLowTransparency()
                            } else {
                                colorScheme.onBackground.withHighTransparency()
                            },
                            shape = RectangleShape,
                        )
                        .clipToBounds(),
                ) {
                    val coverUri = bucket.coverUri
                    SketchesThumbnailAsyncImage(
                        uri = coverUri,
                        contentDescription = stringResource(R.string.bucket_cover),
                        modifier = Modifier.matchParentSize(),
                    )
                    if (bucketSelected) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    color = colorScheme.background.withMediumTransparency(),
                                    shape = RectangleShape,
                                ),
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_media_selected),
                            contentDescription = stringResource(R.string.selected),
                            tint = colorScheme.onBackground,
                            modifier = Modifier
                                .align(alignment = Alignment.TopStart)
                                .padding(all = dimens.mediaGridIconPadding)
                                .dropShadow(
                                    shape = CircleShape,
                                    shadow = Shadow(
                                        radius = dimens.shadowBlurRadius,
                                        color = colorScheme.background.withLowTransparency(),
                                    ),
                                ),
                        )
                    }
                    if (bucket.isHidden) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bucket_hidden),
                            contentDescription = stringResource(R.string.hidden_bucket),
                            tint = colorScheme.onBackground,
                            modifier = Modifier
                                .align(alignment = Alignment.TopEnd)
                                .padding(all = dimens.mediaGridIconPadding)
                                .dropShadow(
                                    shape = CircleShape,
                                    shadow = Shadow(
                                        radius = dimens.shadowBlurRadius,
                                        color = colorScheme.background.withLowTransparency(),
                                    ),
                                ),
                        )
                    }
                }
                Text(
                    text = bucket.name,
                    modifier = Modifier.padding(
                        start = 4.dp,
                        top = 4.dp,
                        end = 4.dp,
                        bottom = 0.dp,
                    ),
                    color = colorScheme.onBackground,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                Text(
                    text = bucket.size.toString(),
                    modifier = Modifier.padding(
                        start = 4.dp,
                        top = 0.dp,
                        end = 4.dp,
                        bottom = 4.dp,
                    ),
                    color = colorScheme.onBackground,
                    fontSize = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun Collection<MediaBucket>.filterByIds(ids: Set<Long>): List<MediaBucket> {
    val size = this.size.coerceAtMost(ids.size)
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

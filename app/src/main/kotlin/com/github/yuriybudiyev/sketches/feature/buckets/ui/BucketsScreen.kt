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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.data.utils.filterByIds
import com.github.yuriybudiyev.sketches.core.navigation.LocalRootNavBarController
import com.github.yuriybudiyev.sketches.core.platform.content.launchDeleteMediaRequest
import com.github.yuriybudiyev.sketches.core.platform.share.LocalShareManager
import com.github.yuriybudiyev.sketches.core.platform.share.toShareInfo
import com.github.yuriybudiyev.sketches.core.saver.SnapshotStateListSaver
import com.github.yuriybudiyev.sketches.core.saver.SnapshotStateSetSaver
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAppBarActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesDeleteConfirmationDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLazyGrid
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.components.rememberSketchesLazyGridState
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsNavRoute
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun BucketsRoute(
    viewModel: BucketsScreenViewModel,
    onBucketClick: (index: Int, bucket: MediaStoreBucket) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateMediaAccess()
    }
    BucketsScreen(
        uiState = uiState,
        onBucketClick = onBucketClick,
        onShareBuckets = { buckets ->
            viewModel.startSharingBuckets(buckets)
        },
        onDeleteBuckets = { buckets ->
            viewModel.startDeletingBuckets(buckets)
        },
        onDeleteMedia = { uris ->
            viewModel.deleteMedia(uris)
        }
    )
}

@Composable
fun BucketsScreen(
    uiState: BucketsScreenViewModel.UiState,
    onBucketClick: (index: Int, bucket: MediaStoreBucket) -> Unit,
    onShareBuckets: (buckets: Collection<MediaStoreBucket>) -> Unit,
    onDeleteBuckets: (buckets: Collection<MediaStoreBucket>) -> Unit,
    onDeleteMedia: (uris: Collection<Uri>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val contextUpdated by rememberUpdatedState(LocalContext.current)
    val shareManagerUpdated by rememberUpdatedState(LocalShareManager.current)
    val onShareBucketsUpdated by rememberUpdatedState(onShareBuckets)
    val onDeleteBucketsUpdated by rememberUpdatedState(onDeleteBuckets)
    val onDeleteMediaUpdated by rememberUpdatedState(onDeleteMedia)
    var allBuckets by remember { mutableStateOf<List<MediaStoreBucket>>(emptyList()) }
    val selectedBuckets =
        rememberSaveable(saver = SnapshotStateSetSaver()) { SnapshotStateSet<Long>() }
    val deleteDialogUris =
        rememberSaveable(saver = SnapshotStateListSaver()) { SnapshotStateList<Uri>() }
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { (resultCode, _) ->
            if (resultCode == Activity.RESULT_OK) {
                coroutineScope.launch {
                    selectedBuckets.clear()
                }
            }
        },
    )
    DisposableEffect(Unit) {
        shareManagerUpdated.registerOnSharedListener(ACTION_SHARE) {
            coroutineScope.launch {
                selectedBuckets.clear()
            }
        }
        onDispose {
            shareManagerUpdated.unregisterOnSharedListener(ACTION_SHARE)
        }
    }
    LaunchedEffect(Unit) {
        if (selectedBuckets.isEmpty()) {
            deleteDialogUris.clear()
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
                    is BucketsScreenViewModel.UiState.Buckets.Action.Share -> {
                        val shareInfo = action.files.toShareInfo()
                        shareManagerUpdated.startChooserActivity(
                            uris = shareInfo.uris,
                            mimeType = shareInfo.mimeType,
                            chooserTitle = contextUpdated.getString(R.string.share_selected),
                            listenerAction = ACTION_SHARE,
                        )
                    }
                    is BucketsScreenViewModel.UiState.Buckets.Action.Delete -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            deleteRequestLauncher.launchDeleteMediaRequest(
                                contextUpdated,
                                action.files.map { file -> file.uri },
                            )
                        } else {
                            if (deleteDialogUris.isNotEmpty()) {
                                deleteDialogUris.clear()
                            }
                            deleteDialogUris.addAll(action.files.map { file -> file.uri })
                        }
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }
        }
    }
    val rootNavBarController = LocalRootNavBarController.current
    LaunchedEffect(rootNavBarController) {
        snapshotFlow { selectedBuckets.toSet().isNotEmpty() }
            .distinctUntilChanged()
            .collect { hasSelectedBuckets ->
                if (hasSelectedBuckets) {
                    rootNavBarController.hideRootNavBar()
                } else {
                    rootNavBarController.showRootNavBar()
                }
            }
    }
    val bucketsGridState = rememberSketchesLazyGridState()
    DisposableEffect(rootNavBarController) {
        rootNavBarController.setOnClickListener(BucketsNavRoute) {
            coroutineScope.launch {
                if (allBuckets.isNotEmpty()) {
                    bucketsGridState.animateScrollToItem(index = 0)
                }
            }
        }
        onDispose {
            rootNavBarController.clearOnClickListener(BucketsNavRoute)
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
                SideEffect {
                    if (selectedBuckets.isNotEmpty()) {
                        selectedBuckets.clear()
                    }
                    if (deleteDialogUris.isNotEmpty()) {
                        deleteDialogUris.clear()
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
                BucketsScreenLayout(
                    state = bucketsGridState,
                    buckets = buckets,
                    selectedBuckets = selectedBuckets,
                    onBucketClick = onBucketClick,
                    modifier = Modifier.matchParentSize(),
                )
            }
            is BucketsScreenViewModel.UiState.Error -> {
                SketchesErrorMessage(
                    thrown = uiState.thrown,
                    modifier = Modifier.matchParentSize()
                )
                SideEffect {
                    if (selectedBuckets.isNotEmpty()) {
                        selectedBuckets.clear()
                    }
                    if (deleteDialogUris.isNotEmpty()) {
                        deleteDialogUris.clear()
                    }
                    if (allBuckets.isNotEmpty()) {
                        allBuckets = emptyList()
                    }
                }
            }
        }
        SketchesTopAppBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            text = if (selectedBuckets.isNotEmpty()) {
                stringResource(
                    R.string.selected_count,
                    selectedBuckets.size,
                )
            } else {
                stringResource(BucketsNavRoute.titleRes)
            },
            backgroundColor = MaterialTheme.colorScheme.background
                .copy(alpha = SketchesColors.UiAlphaLowTransparency),
        ) {
            if (selectedBuckets.isNotEmpty()) {
                SketchesAppBarActionButton(
                    icon = SketchesIcons.Delete,
                    description = stringResource(R.string.delete_selected),
                    onClick = {
                        onDeleteBucketsUpdated(allBuckets.filterByIds(selectedBuckets.toSet()))
                    },
                )
                val shareDescription = stringResource(R.string.share_selected)
                SketchesAppBarActionButton(
                    icon = SketchesIcons.Share,
                    description = shareDescription,
                    onClick = {
                        onShareBucketsUpdated(allBuckets.filterByIds(selectedBuckets.toSet()))
                    },
                )
            }
        }
        if (deleteDialogUris.isNotEmpty()) {
            SketchesDeleteConfirmationDialog(
                onDelete = {
                    onDeleteMediaUpdated(deleteDialogUris.toList())
                    coroutineScope.launch {
                        deleteDialogUris.clear()
                        selectedBuckets.clear()
                    }
                },
                onDismiss = {
                    coroutineScope.launch {
                        deleteDialogUris.clear()
                    }
                },
            )
        }
    }
}

@Composable
private fun BucketsScreenLayout(
    state: LazyGridState,
    buckets: List<MediaStoreBucket>,
    selectedBuckets: SnapshotStateSet<Long>,
    onBucketClick: (index: Int, bucket: MediaStoreBucket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bucketsUpdated by rememberUpdatedState(buckets)
    val selectedBucketsUpdated by rememberUpdatedState(selectedBuckets)
    val onBucketClickUpdated by rememberUpdatedState(onBucketClick)
    SketchesLazyGrid(
        modifier = modifier,
        state = state,
        overlayTop = true,
        overlayBottom = true,
    ) {
        items(
            count = bucketsUpdated.size,
            key = { index -> bucketsUpdated[index].id },
        ) { index ->
            val bucket = bucketsUpdated[index]
            val bucketSelectedOnComposition = selectedBucketsUpdated.contains(bucket.id)
            Column(
                modifier = Modifier
                    .clip(shape = MaterialTheme.shapes.extraSmall)
                    .combinedClickable(
                        onLongClick = {
                            if (selectedBucketsUpdated.isEmpty()) {
                                selectedBucketsUpdated.add(bucket.id)
                            } else {
                                if (selectedBucketsUpdated.contains(bucket.id)) {
                                    selectedBucketsUpdated.clear()
                                } else {
                                    selectedBucketsUpdated.addAll(bucketsUpdated.map { bucket -> bucket.id })
                                }
                            }
                        },
                        onClick = {
                            if (selectedBucketsUpdated.isNotEmpty()) {
                                if (selectedBucketsUpdated.contains(bucket.id)) {
                                    selectedBucketsUpdated.remove(bucket.id)
                                } else {
                                    selectedBucketsUpdated.add(bucket.id)
                                }
                            } else {
                                onBucketClickUpdated(
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
                        .aspectRatio(ratio = 1f)
                        .border(
                            width = SketchesDimens.MediaItemBorderThickness,
                            color = if (bucketSelectedOnComposition) {
                                MaterialTheme.colorScheme.onBackground
                                    .copy(alpha = SketchesColors.UiAlphaLowTransparency)
                            } else {
                                MaterialTheme.colorScheme.onBackground
                                    .copy(alpha = SketchesColors.UiAlphaHighTransparency)
                            },
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                        .clip(shape = MaterialTheme.shapes.extraSmall)
                ) {
                    SketchesAsyncImage(
                        uri = bucket.coverUri,
                        contentDescription = stringResource(R.string.bucket_cover),
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        enableLoadingIndicator = true,
                        enableErrorIndicator = true,
                    )
                    if (bucketSelectedOnComposition) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    color = MaterialTheme.colorScheme.background
                                        .copy(alpha = SketchesColors.UiAlphaHighTransparency),
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
                    color = MaterialTheme.colorScheme.onBackground,
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
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
    }
}

private const val ACTION_SHARE = "com.github.yuriybudiyev.sketches.feature.buckets.ui.ACTION_SHARE"

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
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.component1
import androidx.activity.result.component2
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.data.utils.filterByIds
import com.github.yuriybudiyev.sketches.core.navigation.LocalNavController
import com.github.yuriybudiyev.sketches.core.navigation.collectNavResult
import com.github.yuriybudiyev.sketches.core.platform.bars.LocalSystemBarsController
import com.github.yuriybudiyev.sketches.core.platform.content.launchDeleteMediaRequest
import com.github.yuriybudiyev.sketches.core.platform.share.LocalShareManager
import com.github.yuriybudiyev.sketches.core.platform.share.toShareInfo
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAppBarActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesDeleteConfirmationDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesMediaGrid
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import com.github.yuriybudiyev.sketches.core.ui.utils.scrollToItemClosestEdge
import com.github.yuriybudiyev.sketches.feature.image.ui.NAV_IMAGE_SCREEN_CURRENT_INDEX
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun BucketRoute(
    onImageClick: (index: Int, file: MediaStoreFile) -> Unit,
    viewModel: BucketScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateMediaAccess()
    }
    BucketScreen(
        bucketName = viewModel.navRoute.bucketName,
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
    var allFiles by remember { mutableStateOf<ImmutableList<MediaStoreFile>>(persistentListOf()) }
    val selectedFiles = rememberSaveable { SnapshotStateSet<Long>() }
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
        shareManagerUpdated.registerOnSharedListener(ACTION_SHARE) {
            coroutineScope.launch {
                selectedFiles.clear()
            }
        }
        onDispose {
            shareManagerUpdated.unregisterOnSharedListener(ACTION_SHARE)
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
    val mediaGridState = rememberLazyGridState()
    val navController = LocalNavController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(
        navController,
        lifecycleOwner,
    ) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navController.collectNavResult<Int>(NAV_IMAGE_SCREEN_CURRENT_INDEX) { index ->
                if (index != null) {
                    mediaGridState.scrollToItemClosestEdge(
                        index = index,
                        animate = false,
                    )
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
                        allFiles = persistentListOf()
                    }
                }
            }
            is BucketScreenViewModel.UiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
                SideEffect {
                    if (selectedFiles.isNotEmpty()) {
                        selectedFiles.clear()
                    }
                    if (allFiles.isNotEmpty()) {
                        allFiles = persistentListOf()
                    }
                }
            }
            is BucketScreenViewModel.UiState.Bucket -> {
                val files = uiState.files
                allFiles = files
                BucketScreenLayout(
                    files = files,
                    selectedFiles = selectedFiles,
                    state = mediaGridState,
                    onItemClick = onImageClick,
                    modifier = Modifier.matchParentSize(),
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
                        allFiles = persistentListOf()
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
                SketchesAppBarActionButton(
                    icon = SketchesIcons.Delete,
                    description = stringResource(R.string.delete_selected),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            coroutineScope.launch {
                                deleteRequestLauncher.launchDeleteMediaRequest(
                                    contextUpdated,
                                    allFiles
                                        .filterByIds(selectedFiles.toSet())
                                        .map { file -> file.uri.toUri() },
                                )
                            }
                        } else {
                            deleteDialogVisible = true
                        }
                    },
                )
                val shareDescription = stringResource(R.string.share_selected)
                SketchesAppBarActionButton(
                    icon = SketchesIcons.Share,
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
                                listenerAction = ACTION_SHARE,
                            )
                        }
                    },
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

@Composable
private fun BucketScreenLayout(
    files: ImmutableList<MediaStoreFile>,
    selectedFiles: SnapshotStateSet<Long>,
    state: LazyGridState,
    onItemClick: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        SketchesMediaGrid(
            files = files,
            selectedFiles = selectedFiles,
            onItemClick = onItemClick,
            modifier = Modifier.matchParentSize(),
            state = state,
            overlayTop = true,
            overlayBottom = false,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    WindowInsets.systemBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                )
                .background(
                    MaterialTheme.colorScheme.background
                        .copy(alpha = SketchesColors.UiAlphaLowTransparency),
                    RectangleShape
                )
                .align(Alignment.BottomCenter)
        )
    }
}

private const val ACTION_SHARE = "com.github.yuriybudiyev.sketches.feature.bucket.ui.ACTION_SHARE"

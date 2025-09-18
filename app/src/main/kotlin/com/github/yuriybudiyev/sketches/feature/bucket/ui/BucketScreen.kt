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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.platform.content.launchDeleteMediaRequest
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAlertDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAppBarActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesMediaGrid
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import kotlinx.coroutines.launch

@Composable
fun BucketRoute(
    bucketId: Long,
    bucketName: String?,
    onImageClick: (index: Int, file: MediaStoreFile) -> Unit,
    viewModel: BucketScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(
        bucketId,
        viewModel,
        coroutineScope,
    ) {
        coroutineScope.launch {
            viewModel.updateMedia(bucketId)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        coroutineScope.launch {
            viewModel.updateMediaAccess()
        }
    }
    BucketScreen(
        name = bucketName,
        uiState = uiState,
        onImageClick = onImageClick,
        onDeleteMedia = { files ->
            coroutineScope.launch {
                viewModel.deleteMedia(files)
            }
        }
    )
}

@Composable
fun BucketScreen(
    name: String?,
    uiState: BucketScreenUiState,
    onImageClick: (index: Int, file: MediaStoreFile) -> Unit,
    onDeleteMedia: (files: Collection<MediaStoreFile>) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val contextUpdated by rememberUpdatedState(LocalContext.current)
    val onDeleteMediaUpdated by rememberUpdatedState(onDeleteMedia)
    val selectedFiles = rememberSaveable { SnapshotStateSet<MediaStoreFile>() }
    var deleteDialogVisible by remember { mutableStateOf(false) }
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
    val shareRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { (resultCode, _) ->
            if (resultCode == Activity.RESULT_OK) {
                coroutineScope.launch {
                    selectedFiles.clear()
                }
            }
        },
    )
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is BucketScreenUiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(id = R.string.no_images_found),
                    modifier = Modifier.matchParentSize(),
                )
            }
            is BucketScreenUiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is BucketScreenUiState.Bucket -> {
                BucketScreenLayout(
                    files = uiState.files,
                    selectedFiles = selectedFiles,
                    onItemClick = onImageClick,
                    modifier = Modifier.matchParentSize(),
                )
            }
            is BucketScreenUiState.Error -> {
                SketchesErrorMessage(
                    thrown = uiState.thrown,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        SketchesTopAppBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            text = name,
            backgroundColor = MaterialTheme.colorScheme.background
                .copy(alpha = SketchesColors.UiAlphaLowTransparency),
        ) {
            if (selectedFiles.isNotEmpty()) {
                SketchesAppBarActionButton(
                    icon = SketchesIcons.Delete,
                    description = stringResource(id = R.string.delete_selected),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            coroutineScope.launch {
                                deleteRequestLauncher.launchDeleteMediaRequest(
                                    contextUpdated,
                                    selectedFiles.map { it.uri.toUri() }
                                )
                            }
                        } else {
                            deleteDialogVisible = true
                        }
                    },
                )
            }
        }
        if (deleteDialogVisible) {
            SketchesAlertDialog(
                titleText = stringResource(id = R.string.delete_image_dialog_title),
                contentText = stringResource(id = R.string.delete_selected_images_dialog_content),
                positiveButtonText = stringResource(id = R.string.delete_image_dialog_positive),
                negativeButtonText = stringResource(id = R.string.delete_image_dialog_negative),
                onPositiveResult = {
                    deleteDialogVisible = false
                    onDeleteMediaUpdated(selectedFiles)
                },
                onNegativeResult = {
                    deleteDialogVisible = false
                },
            )
        }
    }
}

@Composable
private fun BucketScreenLayout(
    files: List<MediaStoreFile>,
    selectedFiles: SnapshotStateSet<MediaStoreFile>,
    onItemClick: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        SketchesMediaGrid(
            files = files,
            selectedFiles = selectedFiles,
            onItemClick = onItemClick,
            modifier = Modifier.matchParentSize(),
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

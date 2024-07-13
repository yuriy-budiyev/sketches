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

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.data.model.MediaType
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAlertDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAppBarActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesMediaItem
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.components.media.SketchesMediaPlayer
import com.github.yuriybudiyev.sketches.core.ui.components.media.rememberSketchesMediaState
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import com.github.yuriybudiyev.sketches.core.ui.utils.animateScrollToItemCentered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ImageRoute(
    fileIndex: Int,
    fileId: Long,
    bucketId: Long,
    onShare: (index: Int, file: MediaStoreFile) -> Unit,
    viewModel: ImageScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bucketIdUpdated by rememberUpdatedState(bucketId)
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(
        fileIndex,
        fileId,
        bucketId,
        viewModel,
        coroutineScope,
    ) {
        coroutineScope.launch {
            viewModel.updateMedia(
                fileIndex,
                fileId,
                bucketId,
            )
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        coroutineScope.launch {
            viewModel.updateMediaAccess()
        }
    }
    ImageScreen(
        uiState = uiState,
        coroutineScope,
        { index, file ->
            coroutineScope.launch {
                viewModel.setCurrentMediaData(
                    index,
                    file.id,
                    bucketIdUpdated,
                )
            }
        },
        onDelete = { _, file ->
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                coroutineScope.launch {
                    viewModel.deleteMedia(file.uri)
                }
            }
        },
        onShare = onShare,
    )
}

@Composable
fun ImageScreen(
    uiState: ImageScreenUiState,
    coroutineScope: CoroutineScope,
    onChange: (index: Int, file: MediaStoreFile) -> Unit,
    onDelete: (index: Int, file: MediaStoreFile) -> Unit,
    onShare: (index: Int, file: MediaStoreFile) -> Unit,
) {
    Box(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize()
    ) {
        when (uiState) {
            ImageScreenUiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(id = R.string.no_images_found),
                    modifier = Modifier.matchParentSize(),
                )
            }
            ImageScreenUiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is ImageScreenUiState.Image -> {
                ImageScreenLayout(
                    index = uiState.fileIndex,
                    files = uiState.files,
                    coroutineScope = coroutineScope,
                    onChange = onChange,
                    onDelete = onDelete,
                    onShare = onShare,
                    modifier = Modifier.matchParentSize(),
                )
            }
            is ImageScreenUiState.Error -> {
                SketchesErrorMessage(
                    thrown = uiState.thrown,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
    }
}

@Composable
private fun ImageScreenLayout(
    index: Int,
    files: List<MediaStoreFile>,
    coroutineScope: CoroutineScope,
    onChange: (index: Int, file: MediaStoreFile) -> Unit,
    onDelete: (index: Int, file: MediaStoreFile) -> Unit,
    onShare: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentIndex by remember { mutableIntStateOf(index) }
    val contextUpdated by rememberUpdatedState(LocalContext.current)
    val indexUpdated by rememberUpdatedState(index)
    val filesUpdated by rememberUpdatedState(files)
    val onChangeUpdated by rememberUpdatedState(onChange)
    val onDeleteUpdated by rememberUpdatedState(onDelete)
    val onShareUpdated by rememberUpdatedState(onShare)
    val pagerState = rememberPagerState(currentIndex) { filesUpdated.size }
    val barState = rememberLazyListState(currentIndex)
    val barItemSize = with(LocalDensity.current) { SketchesDimens.MediaBarItemSize.roundToPx() }
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { },
    )
    var deleteDialogVisible by remember { mutableStateOf(false) }
    LaunchedEffect(
        pagerState,
        coroutineScope,
    ) {
        snapshotFlow { indexUpdated }.collect { page ->
            coroutineScope.launch {
                pagerState.scrollToPage(page)
            }
        }
    }
    LaunchedEffect(
        pagerState,
        barState,
        coroutineScope,
    ) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            currentIndex = page
            onChangeUpdated(
                page,
                filesUpdated[page],
            )
            coroutineScope.launch {
                barState.animateScrollToItemCentered(
                    page,
                    barItemSize,
                )
            }
        }
    }
    Box(modifier = modifier) {
        Column(modifier = Modifier.matchParentSize()) {
            MediaPager(
                state = pagerState,
                items = filesUpdated,
                coroutineScope = coroutineScope,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0F),
            )
            MediaBar(
                currentIndex = currentIndex,
                state = barState,
                items = filesUpdated,
                onItemClick = { index, _ ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                modifier = Modifier
                    .height(SketchesDimens.BottomBarHeight)
                    .fillMaxWidth(),
            )
        }
        TopBar(
            onDelete = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    coroutineScope.launch {
                        deleteRequestLauncher.launch(
                            IntentSenderRequest
                                .Builder(
                                    MediaStore.createDeleteRequest(
                                        contextUpdated.contentResolver,
                                        listOf(filesUpdated[currentIndex].uri)
                                    ).intentSender
                                )
                                .build()
                        )
                    }
                } else {
                    deleteDialogVisible = true
                }
            },
            onShare = {
                onShareUpdated(
                    currentIndex,
                    filesUpdated[currentIndex],
                )
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
        )
        if (deleteDialogVisible) {
            SketchesAlertDialog(
                titleText = stringResource(id = R.string.delete_image_dialog_title),
                contentText = stringResource(id = R.string.delete_image_dialog_content),
                positiveButtonText = stringResource(id = R.string.delete_image_dialog_positive),
                negativeButtonText = stringResource(id = R.string.delete_image_dialog_negative),
                onPositiveResult = {
                    deleteDialogVisible = false
                    onDeleteUpdated(
                        currentIndex,
                        filesUpdated[currentIndex]
                    )
                },
                onNegativeResult = {
                    deleteDialogVisible = false
                },
            )
        }
    }
}

@Composable
private fun TopBar(
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SketchesTopAppBar(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = SketchesColors.UiAlphaLowTransparency),
    ) {
        SketchesAppBarActionButton(
            icon = SketchesIcons.Delete,
            description = stringResource(id = R.string.delete_image),
            onClick = onDelete,
        )
        SketchesAppBarActionButton(
            icon = SketchesIcons.Share,
            description = stringResource(id = R.string.share_image),
            onClick = onShare,
        )
    }
}

@Composable
private fun MediaPager(
    state: PagerState,
    items: List<MediaStoreFile>,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val filesUpdated by rememberUpdatedState(items)
    HorizontalPager(
        state = state,
        key = { page -> filesUpdated[page].id },
        modifier = modifier,
    ) { page ->
        val file = filesUpdated[page]
        MediaPage(
            state = state,
            number = page,
            fileUri = file.uri,
            fileType = file.mediaType,
            coroutineScope = coroutineScope,
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
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    when (fileType) {
        MediaType.Image -> {
            ImagePage(
                fileUri = fileUri,
                modifier = modifier,
            )
        }
        MediaType.Video -> {
            VideoPage(
                state = state,
                number = number,
                fileUri = fileUri,
                coroutineScope = coroutineScope,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ImagePage(
    fileUri: Uri,
    modifier: Modifier = Modifier,
) {
    SketchesAsyncImage(
        uri = fileUri,
        contentDescription = stringResource(id = R.string.image),
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.High,
        enableLoadingIndicator = false,
        enableErrorIndicator = true,
    )
}

@Composable
private fun VideoPage(
    state: PagerState,
    number: Int,
    fileUri: Uri,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val numberUpdated by rememberUpdatedState(number)
    val mediaState = rememberSketchesMediaState(coroutineScope)
    DisposableEffect(
        mediaState,
        fileUri
    ) {
        mediaState.open(fileUri)
        onDispose {
            mediaState.close()
        }
    }
    LaunchedEffect(
        coroutineScope,
        mediaState,
        state,
    ) {
        snapshotFlow { state.currentPage }.collect { currentPage ->
            if (currentPage == numberUpdated) {
                coroutineScope.launch {
                    if (mediaState.isVolumeEnabled) {
                        mediaState.disableVolume()
                    }
                    if (!mediaState.isPlaying) {
                        mediaState.play()
                    }
                }
            } else {
                coroutineScope.launch {
                    if (mediaState.isPlaying) {
                        mediaState.stop()
                    }
                    if (mediaState.isVolumeEnabled) {
                        mediaState.disableVolume()
                    }
                }
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        coroutineScope.launch {
            mediaState.pause()
        }
    }
    SketchesMediaPlayer(
        state = mediaState,
        modifier = modifier,
        coroutineScope = coroutineScope,
        enableImagePlaceholder = false,
        enableErrorIndicator = true
    )
}

@Composable
private fun MediaBar(
    currentIndex: Int,
    state: LazyListState,
    items: List<MediaStoreFile>,
    onItemClick: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentIndexUpdated by rememberUpdatedState(currentIndex)
    val itemsUpdated by rememberUpdatedState(items)
    val onItemClickUpdated by rememberUpdatedState(onItemClick)
    LazyRow(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = SketchesDimens.MediaBarItemSpacing),
        horizontalArrangement = Arrangement.spacedBy(
            space = SketchesDimens.MediaBarItemSpacing,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
        flingBehavior = rememberSnapFlingBehavior(state),
    ) {
        items(
            count = itemsUpdated.size,
            key = { position -> itemsUpdated[position].id },
            contentType = { position -> itemsUpdated[position].mediaType },
        ) { position ->
            val file = itemsUpdated[position]
            val smallMaterialShape = MaterialTheme.shapes.small
            SketchesMediaItem(
                uri = file.uri,
                type = file.mediaType,
                videoIconPadding = SketchesDimens.MediaBarVideoIconPadding,
                modifier = Modifier
                    .size(size = SketchesDimens.MediaBarItemSize)
                    .clip(shape = smallMaterialShape)
                    .border(
                        width = SketchesDimens.MediaItemBorderThickness,
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (position == currentIndexUpdated) {
                                SketchesColors.UiAlphaLowTransparency
                            } else {
                                SketchesColors.UiAlphaHighTransparency
                            }
                        ),
                        shape = smallMaterialShape,
                    )
                    .clickable {
                        onItemClickUpdated(
                            position,
                            file,
                        )
                    },
            )
        }
    }
}

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

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import com.github.yuriybudiyev.sketches.core.platform.content.launchDeleteMediaRequest
import com.github.yuriybudiyev.sketches.core.platform.share.LocalShareManager
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAppBarActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesDeleteConfirmationDialog
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.components.media.SketchesMediaPlayer
import com.github.yuriybudiyev.sketches.core.ui.components.media.rememberSketchesMediaState
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import com.github.yuriybudiyev.sketches.core.ui.utils.animateScrollToItemCentered
import kotlinx.coroutines.launch

@Composable
fun ImageRoute(
    fileIndex: Int,
    fileId: Long,
    bucketId: Long,
    viewModel: ImageScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bucketIdUpdated by rememberUpdatedState(bucketId)
    LaunchedEffect(
        fileIndex,
        fileId,
        bucketId,
        viewModel,
    ) {
        viewModel.updateMedia(
            fileIndex,
            fileId,
            bucketId,
        )
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateMediaAccess()
    }
    ImageScreen(
        uiState = uiState,
        onChange = { index, file ->
            viewModel.setCurrentMediaData(
                index,
                file.id,
                bucketIdUpdated,
            )
        },
        onDelete = { _, file ->
            viewModel.deleteMedia(listOf(file))
        },
    )
}

@Composable
fun ImageScreen(
    uiState: ImageScreenUiState,
    onChange: (index: Int, file: MediaStoreFile) -> Unit,
    onDelete: (index: Int, file: MediaStoreFile) -> Unit,
) {
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxSize()
    ) {
        when (uiState) {
            is ImageScreenUiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(id = R.string.no_images_found),
                    modifier = Modifier.matchParentSize(),
                )
            }
            is ImageScreenUiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is ImageScreenUiState.Image -> {
                ImageScreenLayout(
                    index = uiState.fileIndex,
                    files = uiState.files,
                    onChange = onChange,
                    onDelete = onDelete,
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
    onChange: (index: Int, file: MediaStoreFile) -> Unit,
    onDelete: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentIndex by remember { mutableIntStateOf(index) }
    val contextUpdated by rememberUpdatedState(LocalContext.current)
    val shareManagerUpdated by rememberUpdatedState(LocalShareManager.current)
    val indexUpdated by rememberUpdatedState(index)
    val filesUpdated by rememberUpdatedState(files)
    val onChangeUpdated by rememberUpdatedState(onChange)
    val onDeleteUpdated by rememberUpdatedState(onDelete)
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(currentIndex) { filesUpdated.size }
    val barState = rememberLazyListState(currentIndex)
    val barItemSize = with(LocalDensity.current) { SketchesDimens.MediaBarItemSize.roundToPx() }
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { },
    )
    var deleteDialogVisible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        snapshotFlow { indexUpdated }.collect { page ->
            coroutineScope.launch {
                pagerState.scrollToPage(page)
            }
        }
    }
    LaunchedEffect(Unit) {
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
        SketchesTopAppBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.background
                .copy(alpha = SketchesColors.UiAlphaHighTransparency),
        ) {
            SketchesAppBarActionButton(
                icon = SketchesIcons.Delete,
                description = stringResource(id = R.string.delete_image),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        coroutineScope.launch {
                            deleteRequestLauncher.launchDeleteMediaRequest(
                                contextUpdated,
                                listOf(filesUpdated[currentIndex].uri.toUri()),
                            )
                        }
                    } else {
                        deleteDialogVisible = true
                    }
                },
            )
            val shareDescription = stringResource(id = R.string.share_image)
            SketchesAppBarActionButton(
                icon = SketchesIcons.Share,
                description = shareDescription,
                onClick = {
                    coroutineScope.launch {
                        val file = filesUpdated[currentIndex]
                        shareManagerUpdated.startChooserActivity(
                            file.uri.toUri(),
                            file.mimeType,
                            shareDescription,
                        )
                    }
                },
            )
        }
        if (deleteDialogVisible) {
            SketchesDeleteConfirmationDialog(
                onDelete = {
                    deleteDialogVisible = false
                    onDeleteUpdated(
                        currentIndex,
                        filesUpdated[currentIndex],
                    )
                },
                onDismiss = {
                    deleteDialogVisible = false
                },
            )
        }
    }
}

@Composable
private fun MediaPager(
    state: PagerState,
    items: List<MediaStoreFile>,
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
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
@NonRestartableComposable
private fun MediaPage(
    state: PagerState,
    number: Int,
    fileUri: String,
    fileType: MediaType,
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
                modifier = modifier,
            )
        }
    }
}

@Composable
@NonRestartableComposable
private fun ImagePage(
    fileUri: String,
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
    fileUri: String,
    modifier: Modifier = Modifier,
) {
    val numberUpdated by rememberUpdatedState(number)
    val mediaState = rememberSketchesMediaState()
    DisposableEffect(fileUri) {
        mediaState.open(fileUri)
        onDispose {
            mediaState.close()
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.currentPage }.collect { currentPage ->
            if (currentPage == numberUpdated) {
                mediaState.coroutineScope.launch {
                    if (mediaState.isVolumeEnabled) {
                        mediaState.disableVolume()
                    }
                    if (!mediaState.isPlaying) {
                        mediaState.play()
                    }
                }
            } else {
                mediaState.coroutineScope.launch {
                    if (mediaState.isPlaying) {
                        mediaState.pause()
                    }
                    if (mediaState.isVolumeEnabled) {
                        mediaState.disableVolume()
                    }
                }
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        mediaState.coroutineScope.launch {
            mediaState.pause()
        }
    }
    SketchesMediaPlayer(
        state = mediaState,
        modifier = modifier,
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
            Box(
                modifier = Modifier
                    .size(size = SketchesDimens.MediaBarItemSize)
                    .border(
                        width = SketchesDimens.MediaItemBorderThickness,
                        color = if (position == currentIndexUpdated) {
                            MaterialTheme.colorScheme.onBackground
                                .copy(alpha = SketchesColors.UiAlphaLowTransparency)
                        } else {
                            MaterialTheme.colorScheme.onBackground
                                .copy(alpha = SketchesColors.UiAlphaHighTransparency)
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                    )
                    .clip(shape = MaterialTheme.shapes.extraSmall)
                    .clickable {
                        onItemClickUpdated(
                            position,
                            file,
                        )
                    },
            ) {
                SketchesAsyncImage(
                    uri = file.uri,
                    contentDescription = stringResource(
                        id = when (file.mediaType) {
                            MediaType.Image -> R.string.image
                            MediaType.Video -> R.string.video
                        },
                    ),
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Low,
                    enableLoadingIndicator = true,
                    enableErrorIndicator = true,
                )
                if (file.mediaType == MediaType.Video) {
                    Box(
                        modifier = Modifier
                            .align(alignment = Alignment.BottomStart)
                            .padding(all = SketchesDimens.MediaBarVideoIconPadding)
                            .background(
                                color = MaterialTheme.colorScheme.background
                                    .copy(alpha = SketchesColors.UiAlphaHighTransparency),
                                shape = CircleShape
                            ),
                    ) {
                        Icon(
                            imageVector = SketchesIcons.Video,
                            contentDescription = stringResource(id = R.string.video),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }
    }
}

/*
 * MIT License
 *
 * Copyright (c) 2023 Yuriy Budiyev
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.data.model.MediaType
import com.github.yuriybudiyev.sketches.core.ui.component.AppBarActionButton
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesMediaItem
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.component.media.SketchesMediaPlayer
import com.github.yuriybudiyev.sketches.core.ui.component.media.rememberSketchesMediaState
import com.github.yuriybudiyev.sketches.core.ui.effect.LifecycleEventEffect
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons
import com.github.yuriybudiyev.sketches.core.util.ui.animateScrollToItemCentered

@Composable
fun ImageRoute(
    fileIndex: Int,
    fileId: Long,
    bucketId: Long,
    onShare: (index: Int, file: MediaStoreFile) -> Unit,
    viewModel: ImageScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentFileIndex by rememberSaveable { mutableIntStateOf(fileIndex) }
    var currentFileId by rememberSaveable { mutableLongStateOf(fileId) }
    val coroutineScope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        coroutineScope.launch {
            viewModel.updateImages(
                fileIndex = currentFileIndex,
                fileId = currentFileId,
                bucketId = bucketId,
            )
        }
    }
    ImageScreen(
        uiState = uiState,
        coroutineScope,
        { index, file ->
            currentFileIndex = index
            currentFileId = file.id
        },
        onDelete = { index, file ->

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
    Box(modifier = Modifier.fillMaxSize()) {
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
                SketchesCenteredMessage(
                    text = stringResource(id = R.string.unexpected_error),
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ImageScreenLayout(
    index: Int,
    files: List<MediaStoreFile>,
    coroutineScope: CoroutineScope,
    onChange: (index: Int, file: MediaStoreFile) -> Unit,
    onDelete: (index: Int, file: MediaStoreFile) -> Unit,
    onShare: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentIndex by remember(index) { mutableIntStateOf(index) }
    var currentFile by remember(
        index,
        files,
    ) {
        mutableStateOf(files[index])
    }
    val indexUpdated by rememberUpdatedState(index)
    val filesUpdated by rememberUpdatedState(files)
    val onChangeUpdated by rememberUpdatedState(onChange)
    val onDeleteUpdated by rememberUpdatedState(onDelete)
    val onShareUpdated by rememberUpdatedState(onShare)
    val pagerState = rememberPagerState(currentIndex) { filesUpdated.size }
    val barState = rememberLazyListState(currentIndex)
    val barItemSize = 56.dp
    val barItemSizePx = with(LocalDensity.current) { barItemSize.roundToPx() }
    LaunchedEffect(
        pagerState,
        coroutineScope
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
            val file = filesUpdated[page]
            currentFile = file
            coroutineScope.launch {
                onChangeUpdated(
                    page,
                    file
                )
            }
            coroutineScope.launch {
                barState.animateScrollToItemCentered(
                    page,
                    barItemSizePx
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
                    .weight(1f),
            )
            MediaBar(
                state = barState,
                items = filesUpdated,
                itemSize = barItemSize,
                onItemClick = { index, _ ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                coroutineScope = coroutineScope,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            )
        }
        TopBar(
            onDelete = {
                coroutineScope.launch {
                    onDeleteUpdated(
                        currentIndex,
                        currentFile,
                    )
                }
            },
            onShare = {
                onShareUpdated(
                    currentIndex,
                    currentFile,
                )
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun TopBar(
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onDeleteUpdated by rememberUpdatedState(onDelete)
    val onShareUpdated by rememberUpdatedState(onShare)
    val barScope = rememberCoroutineScope()
    SketchesTopAppBar(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
    ) {
        AppBarActionButton(
            icon = SketchesIcons.Delete,
            description = stringResource(id = R.string.delete_image),
            onClick = {
                barScope.launch {
                    onDeleteUpdated()
                }
            },
        )
        AppBarActionButton(
            icon = SketchesIcons.Share,
            description = stringResource(id = R.string.share_image),
            onClick = {
                barScope.launch {
                    onShareUpdated()
                }
            },
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
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
@OptIn(ExperimentalFoundationApi::class)
private fun MediaPage(
    state: PagerState,
    number: Int,
    fileUri: Uri,
    fileType: MediaType,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    when (fileType) {
        MediaType.IMAGE -> {
            ImagePage(
                fileUri = fileUri,
                modifier = modifier,
            )
        }
        MediaType.VIDEO -> {
            VideoPage(
                state,
                number,
                fileUri = fileUri,
                coroutineScope,
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
        description = stringResource(id = R.string.image),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.High,
        modifier = modifier,
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun VideoPage(
    state: PagerState,
    number: Int,
    fileUri: Uri,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val numberUpdated by rememberUpdatedState(number)
    val fileUriUpdated by rememberUpdatedState(fileUri)
    val mediaState = rememberSketchesMediaState()
    LaunchedEffect(
        mediaState,
        coroutineScope,
    ) {
        snapshotFlow { fileUriUpdated }.collect { uri ->
            if (mediaState.uri != uri) {
                coroutineScope.launch {
                    mediaState.open(uri)
                }
            }
        }
    }
    LaunchedEffect(
        mediaState,
        state,
        coroutineScope,
    ) {
        snapshotFlow { state.currentPage }.collect { currentPage ->
            if (currentPage == numberUpdated) {
                coroutineScope.launch {
                    mediaState.disableVolume()
                    mediaState.play()
                }
            } else {
                coroutineScope.launch {
                    mediaState.stop()
                    mediaState.disableVolume()
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
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MediaBar(
    state: LazyListState,
    items: List<MediaStoreFile>,
    itemSize: Dp,
    onItemClick: (index: Int, file: MediaStoreFile) -> Unit,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    val itemsUpdated by rememberUpdatedState(items)
    val onItemClickUpdated by rememberUpdatedState(onItemClick)
    LazyRow(
        state = state,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
        flingBehavior = rememberSnapFlingBehavior(state),
    ) {
        items(
            count = itemsUpdated.size,
            key = { position -> itemsUpdated[position].id },
        ) { position ->
            val file = itemsUpdated[position]
            SketchesMediaItem(
                uri = file.uri,
                type = file.mediaType,
                iconPadding = 2.dp,
                modifier = Modifier
                    .size(itemSize)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable {
                        coroutineScope.launch {
                            onItemClickUpdated(
                                position,
                                file,
                            )
                        }
                    },
            )
        }
    }
}

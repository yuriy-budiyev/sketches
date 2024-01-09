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
import kotlinx.coroutines.flow.distinctUntilChanged
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
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
    viewModel: ImageScreenViewModel = hiltViewModel(),
    onShare: (uri: Uri, type: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentFileIndex by rememberSaveable { mutableIntStateOf(fileIndex) }
    var currentFileId by rememberSaveable { mutableLongStateOf(fileId) }
    val imageRouteScope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        imageRouteScope.launch {
            viewModel.updateImages(
                fileIndex = currentFileIndex,
                fileId = currentFileId,
                bucketId = bucketId
            )
        }
    }
    ImageScreen(
        uiState = uiState,
        { index, id ->
            currentFileIndex = index
            currentFileId = id
        },
        onShare = onShare
    )
}

@Composable
fun ImageScreen(
    uiState: ImageScreenUiState,
    onChange: (index: Int, id: Long) -> Unit,
    onShare: (uri: Uri, type: String) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            ImageScreenUiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(id = R.string.no_images_found),
                    modifier = Modifier.matchParentSize()
                )
            }
            ImageScreenUiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is ImageScreenUiState.Image -> {
                ImageScreenLayout(
                    index = uiState.fileIndex,
                    files = uiState.files,
                    onChange = onChange,
                    onShare = onShare,
                    modifier = Modifier.matchParentSize()
                )
            }
            is ImageScreenUiState.Error -> {
                SketchesCenteredMessage(
                    text = stringResource(id = R.string.unexpected_error),
                    modifier = Modifier.matchParentSize()
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
    onChange: (index: Int, id: Long) -> Unit,
    onShare: (uri: Uri, type: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val indexUpdated by rememberUpdatedState(index)
    val filesUpdated by rememberUpdatedState(files)
    val pagerState = rememberPagerState(indexUpdated) { filesUpdated.size }
    val bottomBarState = rememberLazyListState(indexUpdated)
    val imageScreenScope = rememberCoroutineScope()
    var currentMediaFile by remember { mutableStateOf(filesUpdated[indexUpdated]) }
    val bottomBarItemSize = 56.dp
    val bottomBarItemSizePx = with(LocalDensity.current) { bottomBarItemSize.roundToPx() }
    LaunchedEffect(
        pagerState,
        bottomBarState,
        imageScreenScope,
        filesUpdated
    ) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val file = filesUpdated[page]
                currentMediaFile = file
                imageScreenScope.launch {
                    onChange(
                        page,
                        file.id
                    )
                }
                imageScreenScope.launch {
                    bottomBarState.animateScrollToItemCentered(
                        page,
                        bottomBarItemSizePx
                    )
                }
            }
    }
    LaunchedEffect(
        pagerState,
        imageScreenScope,
        indexUpdated
    ) {
        snapshotFlow { indexUpdated }
            .distinctUntilChanged()
            .collect { page ->
                imageScreenScope.launch {
                    pagerState.scrollToPage(page)
                }
            }
    }
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.matchParentSize(),
                key = { page -> filesUpdated[page].id },
            ) { page ->
                val file = filesUpdated[page]
                val fileUri = file.uri
                when (file.mediaType) {
                    MediaType.IMAGE -> {
                        SketchesAsyncImage(
                            uri = fileUri,
                            description = stringResource(id = R.string.image),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.High
                        )
                    }
                    MediaType.VIDEO -> {
                        val mediaState = rememberSketchesMediaState()
                        SketchesMediaPlayer(
                            state = mediaState,
                            modifier = Modifier.fillMaxSize()
                        )
                        LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
                            if (mediaState.isPlaying) {
                                mediaState.pause()
                            }
                        }
                        LaunchedEffect(
                            mediaState,
                            imageScreenScope,
                            fileUri
                        ) {
                            if (mediaState.uri != fileUri) {
                                imageScreenScope.launch {
                                    mediaState.open(fileUri)
                                }
                            }
                        }
                        LaunchedEffect(
                            pagerState,
                            mediaState,
                            imageScreenScope,
                            page
                        ) {
                            snapshotFlow { pagerState.currentPage }
                                .distinctUntilChanged()
                                .collect { currentPage ->
                                    if (page == currentPage) {
                                        imageScreenScope.launch {
                                            mediaState.disableVolume()
                                            mediaState.play()
                                        }
                                    } else {
                                        imageScreenScope.launch {
                                            mediaState.stop()
                                            mediaState.disableVolume()
                                        }
                                    }
                                }
                        }
                    }
                }
            }
            SketchesTopAppBar(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.75f)
            ) {
                AppBarActionButton(
                    icon = SketchesIcons.Delete,
                    description = stringResource(id = R.string.delete_image),
                    onClick = {

                    },
                )
                AppBarActionButton(
                    icon = SketchesIcons.Share,
                    description = stringResource(id = R.string.share_image),
                    onClick = {
                        val file = currentMediaFile
                        imageScreenScope.launch {
                            onShare(
                                file.uri,
                                file.mimeType
                            )
                        }
                    },
                )
            }
        }
        LazyRow(
            state = bottomBarState,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically,
            flingBehavior = rememberSnapFlingBehavior(bottomBarState),
        ) {
            items(
                count = filesUpdated.size,
                key = { page -> filesUpdated[page].id },
            ) { page ->
                val file = filesUpdated[page]
                SketchesMediaItem(
                    uri = file.uri,
                    type = file.mediaType,
                    iconPadding = 2.dp,
                    modifier = Modifier
                        .size(bottomBarItemSize)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onBackground,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            imageScreenScope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        },
                )
            }
        }
    }
}

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

package com.github.yuriybudiyev.sketches.image.ui

import android.net.Uri
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.component.media.SketchesMediaPlayer
import com.github.yuriybudiyev.sketches.core.ui.component.media.rememberSketchesMediaState
import com.github.yuriybudiyev.sketches.core.ui.effect.LifecycleEventEffect
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons
import com.github.yuriybudiyev.sketches.core.util.ui.animateScrollToItemCentered
import com.github.yuriybudiyev.sketches.images.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.images.ui.component.SketchesMediaItem

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
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateImages(
            imageIndex = currentFileIndex,
            imageId = currentFileId,
            bucketId = bucketId
        )
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
    when (uiState) {
        ImageScreenUiState.Empty -> {
            SketchesCenteredMessage(text = stringResource(id = R.string.no_images_found))
        }
        ImageScreenUiState.Loading -> {
            SketchesLoadingIndicator()
        }
        is ImageScreenUiState.Image -> {
            ImageScreenLayout(
                index = uiState.fileIndex,
                files = uiState.files,
                onChange = onChange,
                onShare = onShare
            )
        }
        is ImageScreenUiState.Error -> {
            SketchesCenteredMessage(text = stringResource(id = R.string.unexpected_error))
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
) {
    val data by rememberUpdatedState(files)
    val pagerState = rememberPagerState(index) { data.size }
    val bottomBarState = rememberLazyListState(index)
    val imageScreenScope = rememberCoroutineScope()
    var currentMediaFile by remember { mutableStateOf(data[index]) }
    val bottomBarItemSize = 64.dp
    val bottomBarItemSizePx = with(LocalDensity.current) { bottomBarItemSize.roundToPx() }
    LaunchedEffect(
        pagerState,
        bottomBarState,
        imageScreenScope,
        data
    ) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val file = data[page]
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
        index
    ) {
        snapshotFlow { index }
            .distinctUntilChanged()
            .collect { page ->
                imageScreenScope.launch {
                    pagerState.scrollToPage(page)
                }
            }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.matchParentSize(),
                key = { page -> data[page].id },
            ) { page ->
                val file = data[page]
                val fileUri = file.uri
                when (file.type) {
                    MediaStoreFile.Type.IMAGE -> {
                        SketchesAsyncImage(
                            uri = fileUri,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.High
                        )
                    }
                    MediaStoreFile.Type.VIDEO -> {
                        val mediaState = rememberSketchesMediaState()
                        val mediaScope = rememberCoroutineScope()
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
                            mediaScope,
                            fileUri
                        ) {
                            if (mediaState.uri != fileUri) {
                                mediaScope.launch {
                                    mediaState.open(fileUri)
                                }
                            }
                        }
                        LaunchedEffect(
                            pagerState,
                            mediaState,
                            mediaScope,
                            page
                        ) {
                            snapshotFlow { pagerState.currentPage }
                                .distinctUntilChanged()
                                .collect { currentPage ->
                                    if (page == currentPage) {
                                        mediaScope.launch {
                                            mediaState.play()
                                        }
                                    } else {
                                        mediaScope.launch {
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
                backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
            ) {
                Box(
                    modifier = Modifier
                        .size(size = 48.dp)
                        .clip(shape = CircleShape)
                        .clickable {
                            val file = currentMediaFile
                            imageScreenScope.launch {
                                onShare(
                                    file.uri,
                                    file.mimeType
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SketchesIcons.Share,
                        contentDescription = null,
                        modifier = Modifier.size(size = 24.dp)
                    )
                }
            }
        }
        LazyRow(
            state = bottomBarState,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(
                space = 4.dp,
                alignment = Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically,
            flingBehavior = rememberSnapFlingBehavior(bottomBarState),
        ) {
            items(
                count = data.size,
                key = { page -> data[page].id },
            ) { page ->
                SketchesMediaItem(
                    file = data[page],
                    iconPadding = 2.dp,
                    modifier = Modifier
                        .size(bottomBarItemSize)
                        .clip(RoundedCornerShape(8.dp))
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

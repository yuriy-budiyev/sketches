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

import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import com.github.yuriybudiyev.sketches.core.ui.component.media.MediaPlayer
import com.github.yuriybudiyev.sketches.core.ui.effect.LifecycleEventEffect
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons
import com.github.yuriybudiyev.sketches.images.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.images.ui.component.SketchesMediaItem

typealias ImageShareListener = (image: MediaStoreFile) -> Unit

typealias ImageChangeListener = (index: Int, image: MediaStoreFile) -> Unit

@Composable
fun ImageRoute(
    imageIndex: Int,
    imageId: Long,
    bucketId: Long,
    viewModel: ImageScreenViewModel = hiltViewModel(),
    onImageShare: ImageShareListener
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentImageIndex by rememberSaveable { mutableIntStateOf(imageIndex) }
    var currentImageId by rememberSaveable { mutableLongStateOf(imageId) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateImages(
            imageIndex = currentImageIndex,
            imageId = currentImageId,
            bucketId = bucketId
        )
    }
    ImageScreen(
        uiState = uiState,
        { index, image ->
            currentImageIndex = index
            currentImageId = image.id
        },
        onImageShare = onImageShare
    )
}

@Composable
fun ImageScreen(
    uiState: ImageScreenUiState,
    onImageChanged: ImageChangeListener,
    onImageShare: ImageShareListener
) {
    when (uiState) {
        ImageScreenUiState.Empty -> {
            SketchesCenteredMessage(text = stringResource(id = R.string.no_images_found))
        }
        ImageScreenUiState.Loading -> {
            SketchesLoadingIndicator()
        }
        is ImageScreenUiState.Image -> {
            ImageLayout(
                index = uiState.imageIndex,
                images = uiState.images,
                onImageChanged = onImageChanged,
                onImageShare = onImageShare
            )
        }
        is ImageScreenUiState.Error -> {
            SketchesCenteredMessage(text = stringResource(id = R.string.unexpected_error))
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ImageLayout(
    index: Int,
    images: List<MediaStoreFile>,
    onImageChanged: ImageChangeListener,
    onImageShare: ImageShareListener,
) {
    val data by rememberUpdatedState(newValue = images)
    val context by rememberUpdatedState(newValue = LocalContext.current)
    val pagerState = rememberPagerState(initialPage = index) { data.size }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = index)
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(
        data,
        pagerState,
        listState,
        coroutineScope
    ) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val image = data[page]
            coroutineScope.launch {
                onImageChanged(
                    page,
                    image
                )
            }
            val scrollOffset = calculateScrollOffset(listState)
            coroutineScope.launch {
                listState.animateScrollToItem(
                    page,
                    scrollOffset
                )
            }
        }
    }
    LaunchedEffect(
        index,
        pagerState
    ) {
        snapshotFlow { index }.collect { index ->
            pagerState.scrollToPage(index)
        }
    }
    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            SketchesTopAppBar(actions = {
                Box(modifier = Modifier
                    .size(size = 48.dp)
                    .clip(shape = CircleShape)
                    .clickable {
                        val image = data[pagerState.currentPage]
                        coroutineScope.launch {
                            onImageShare(image)
                        }
                    },
                    contentAlignment = Alignment.Center,
                    content = {
                        Icon(
                            imageVector = SketchesIcons.Share,
                            contentDescription = null,
                            modifier = Modifier.size(size = 24.dp)
                        )
                    })
            })
        },
        bottomBar = {
            LazyRow(state = listState,
                modifier = Modifier
                    .height(height = 96.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                content = {
                    items(count = data.size,
                        key = { item -> data[item].id },
                        itemContent = { item ->
                            SketchesMediaItem(file = data[item],
                                iconPadding = 2.dp,
                                modifier = Modifier
                                    .aspectRatio(
                                        ratio = 1f,
                                        matchHeightConstraintsFirst = true
                                    )
                                    .clip(shape = RoundedCornerShape(8.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(item)
                                        }
                                    })
                        })
                })
        },
        content = { contentPadding ->
            HorizontalPager(state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                pageSpacing = 8.dp,
                key = { page -> data[page].id },
                pageContent = { page ->
                    val file = data[page]
                    when (file.type) {
                        MediaStoreFile.Type.IMAGE -> {
                            SketchesAsyncImage(
                                uri = file.uri,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.High
                            )
                        }
                        MediaStoreFile.Type.VIDEO -> {
                            MediaPlayer(
                                uri = file.uri,
                                Modifier.fillMaxSize()
                            )
                        }
                    }
                })
        })
}

/**
 * Works only if all items have the same size.
 */
private fun calculateScrollOffset(listState: LazyListState): Int {
    val layoutInfo = listState.layoutInfo
    val itemsInfo = layoutInfo.visibleItemsInfo
    return if (itemsInfo.isNotEmpty()) {
        val viewportSize = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
        itemsInfo.first().size / 2 - viewportSize / 2
    } else {
        0
    }
}

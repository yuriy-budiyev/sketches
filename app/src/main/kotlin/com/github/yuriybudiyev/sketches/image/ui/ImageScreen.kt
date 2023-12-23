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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesImage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.effect.LifecycleEventEffect
import com.github.yuriybudiyev.sketches.images.data.model.MediaStoreImage

typealias ImageShareListener = (image: MediaStoreImage) -> Unit

typealias ImageChangeListener = (index: Int, image: MediaStoreImage) -> Unit

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
    images: List<MediaStoreImage>,
    onImageChanged: ImageChangeListener,
    onImageShare: ImageShareListener,
) {
    val data by rememberUpdatedState(images)
    val pagerState = rememberPagerState(index) { data.size }
    val listState = rememberLazyListState()
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onImageChanged(
                page,
                data[page]
            )
        }
    }
    LaunchedEffect(index) {
        pagerState.scrollToPage(index)
    }
    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {

        },
        bottomBar = {
            LazyRow(state = listState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
                content = {
                    items(count = data.size,
                        key = { page -> data[page].id },
                        itemContent = { page ->
                            val image = data[page]
                            SketchesImage(uri = image.uri,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(ratio = 1f)
                                    .clip(shape = RoundedCornerShape(8.dp))
                                    .clickable {

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
                    SketchesImage(
                        uri = data[page].uri,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.High
                    )
                })
        })
}

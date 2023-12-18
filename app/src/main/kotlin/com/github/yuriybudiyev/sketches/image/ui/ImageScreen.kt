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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
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

@Composable
fun ImageRoute(
    imageIndex: Int,
    imageId: Long,
    bucketId: Long,
    viewModel: ImageScreenViewModel = hiltViewModel(),
    onImageShare: ImageShareListener
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateImages(
            imageIndex = imageIndex,
            imageId = imageId,
            bucketId = bucketId
        )
    }
    ImageScreen(
        uiState = uiState,
        onImageShare = onImageShare
    )
}

@Composable
fun ImageScreen(
    uiState: ImageScreenUiState,
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
            ImagePager(
                imageIndex = uiState.imageIndex,
                images = uiState.images,
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
fun ImagePager(
    imageIndex: Int,
    images: List<MediaStoreImage>,
    onImageShare: ImageShareListener
) {
    val pagerState = rememberPagerState(imageIndex) { images.size }
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        SketchesImage(
            uri = images[page].uri,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High
        )
    }
}

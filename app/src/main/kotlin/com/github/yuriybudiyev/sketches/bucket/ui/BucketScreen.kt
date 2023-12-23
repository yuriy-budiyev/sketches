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

package com.github.yuriybudiyev.sketches.bucket.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.effect.LifecycleEventEffect
import com.github.yuriybudiyev.sketches.images.ui.component.ImageClickListener
import com.github.yuriybudiyev.sketches.images.ui.component.SketchesAsyncImageVerticalGrid

@Composable
fun BucketRoute(
    id: Long,
    name: String?,
    onImageClick: ImageClickListener,
    viewModel: BucketScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateImages(id)
    }
    BucketScreen(
        name = name,
        uiState = uiState,
        onImageClick = onImageClick
    )
}

@Composable
fun BucketScreen(
    name: String?,
    uiState: BucketScreenUiState,
    onImageClick: ImageClickListener
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (!name.isNullOrEmpty()) {
            SketchesTopAppBar(text = name)
        }
        when (uiState) {
            BucketScreenUiState.Empty -> {
                SketchesCenteredMessage(text = stringResource(id = R.string.no_images_found))
            }
            BucketScreenUiState.Loading -> {
                SketchesLoadingIndicator()
            }
            is BucketScreenUiState.Bucket -> {
                SketchesAsyncImageVerticalGrid(
                    images = uiState.images,
                    onImageClick = onImageClick
                )
            }
            is BucketScreenUiState.Error -> {
                SketchesCenteredMessage(text = stringResource(id = R.string.unexpected_error))
            }
        }
    }
}

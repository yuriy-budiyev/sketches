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

package com.github.yuriybudiyev.sketches.feature.bucket.ui

import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.component.MediaItemClickListener
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesMediaVerticalGrid
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.effect.LifecycleEventEffect

@Composable
fun BucketRoute(
    id: Long,
    name: String?,
    onImageClick: MediaItemClickListener,
    viewModel: BucketScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bucketRouteScope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        bucketRouteScope.launch {
            viewModel.updateImages(id)
        }
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
    onImageClick: MediaItemClickListener,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            BucketScreenUiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(id = R.string.no_images_found),
                    modifier = Modifier.matchParentSize()
                )
            }
            BucketScreenUiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is BucketScreenUiState.Bucket -> {
                SketchesMediaVerticalGrid(
                    files = uiState.files,
                    modifier = Modifier.matchParentSize(),
                    onItemClick = onImageClick
                )
            }
            is BucketScreenUiState.Error -> {
                SketchesCenteredMessage(
                    text = stringResource(id = R.string.unexpected_error),
                    modifier = Modifier.matchParentSize()
                )
            }
        }
        SketchesTopAppBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            text = name,
            backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.75f)
        )
    }
}

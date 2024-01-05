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

package com.github.yuriybudiyev.sketches.buckets.ui

import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.buckets.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesLazyVerticalGrid
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.effect.LifecycleEventEffect

typealias BucketClickListener = (index: Int, bucket: MediaStoreBucket) -> Unit

@Composable
fun BucketsRoute(
    onBucketClick: BucketClickListener,
    viewModel: BucketsScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.updateBuckets()
    }
    BucketsScreen(
        uiState = uiState,
        onBucketClick = onBucketClick
    )
}

@Composable
fun BucketsScreen(
    uiState: BucketsScreenUiState,
    onBucketClick: BucketClickListener,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SketchesTopAppBar(text = stringResource(id = R.string.buckets_screen_label))
        when (uiState) {
            BucketsScreenUiState.Empty -> {
                SketchesCenteredMessage(text = stringResource(id = R.string.no_buckets_found))
            }
            BucketsScreenUiState.Loading -> {
                SketchesLoadingIndicator()
            }
            is BucketsScreenUiState.Buckets -> {
                BucketsLayout(
                    buckets = uiState.buckets,
                    onBucketClick = onBucketClick
                )
            }
            is BucketsScreenUiState.Error -> {
                SketchesCenteredMessage(text = stringResource(id = R.string.unexpected_error))
            }
        }
    }
}

@Composable
private fun BucketsLayout(
    buckets: List<MediaStoreBucket>,
    onBucketClick: BucketClickListener,
) {
    val data by rememberUpdatedState(buckets)
    val coroutineScope = rememberCoroutineScope()
    SketchesLazyVerticalGrid(modifier = Modifier.fillMaxSize()) {
        items(count = data.size,
            key = { index -> data[index].id },
            itemContent = { index ->
                val item = data[index]
                Column(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(8.dp))
                        .clickable {
                            coroutineScope.launch {
                                onBucketClick(
                                    index,
                                    item
                                )
                            }
                        },
                ) {
                    SketchesAsyncImage(
                        uri = item.coverUri,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratio = 1f)
                            .clip(shape = RoundedCornerShape(8.dp))
                    )
                    Text(
                        text = item.name,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(
                            start = 4.dp,
                            top = 4.dp,
                            end = 4.dp,
                            bottom = 0.dp
                        )
                    )
                    Text(
                        text = item.size.toString(),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(
                            start = 4.dp,
                            top = 0.dp,
                            end = 4.dp,
                            bottom = 4.dp
                        )
                    )
                }
            })
    }
}

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

package com.github.yuriybudiyev.sketches.feature.buckets.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLazyVerticalGrid
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsRoute
import kotlinx.coroutines.launch

@Composable
fun BucketsRoute(
    onBucketClick: (index: Int, bucket: MediaStoreBucket) -> Unit,
    viewModel: BucketsScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(
        viewModel,
        coroutineScope,
    ) {
        coroutineScope.launch {
            viewModel.updateBuckets()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        coroutineScope.launch {
            viewModel.updateMediaAccess()
        }
    }
    BucketsScreen(
        uiState = uiState,
        onBucketClick = onBucketClick,
    )
}

@Composable
fun BucketsScreen(
    uiState: BucketsScreenUiState,
    onBucketClick: (index: Int, bucket: MediaStoreBucket) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is BucketsScreenUiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(id = R.string.no_buckets_found),
                    modifier = Modifier.matchParentSize(),
                )
            }
            is BucketsScreenUiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is BucketsScreenUiState.Buckets -> {
                BucketsScreenLayout(
                    buckets = uiState.buckets,
                    onBucketClick = onBucketClick,
                    modifier = Modifier.matchParentSize(),
                )
            }
            is BucketsScreenUiState.Error -> {
                SketchesErrorMessage(
                    thrown = uiState.thrown,
                    modifier = Modifier.matchParentSize()
                )
            }
        }
        SketchesTopAppBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            text = stringResource(id = BucketsRoute.titleRes),
            backgroundColor = MaterialTheme.colorScheme.background
                .copy(alpha = SketchesColors.UiAlphaLowTransparency),
        )
    }
}

@Composable
private fun BucketsScreenLayout(
    buckets: List<MediaStoreBucket>,
    onBucketClick: (index: Int, bucket: MediaStoreBucket) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bucketsUpdated by rememberUpdatedState(buckets)
    val onBucketClickUpdated by rememberUpdatedState(onBucketClick)
    SketchesLazyVerticalGrid(
        modifier = modifier,
        overlayTop = true,
        overlayBottom = true,
    ) {
        items(
            count = bucketsUpdated.size,
            key = { index -> bucketsUpdated[index].id },
        ) { index ->
            val item = bucketsUpdated[index]
            Column(
                modifier = Modifier
                    .clip(shape = MaterialTheme.shapes.small)
                    .clickable {
                        onBucketClickUpdated(
                            index,
                            item,
                        )
                    },
            ) {
                val colorScheme = MaterialTheme.colorScheme
                SketchesAsyncImage(
                    uri = item.coverUri,
                    contentDescription = stringResource(id = R.string.bucket_cover),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio = 1.0F)
                        .clip(shape = MaterialTheme.shapes.small)
                        .border(
                            width = SketchesDimens.MediaItemBorderThickness,
                            color = colorScheme.onBackground
                                .copy(alpha = SketchesColors.UiAlphaHighTransparency),
                            shape = MaterialTheme.shapes.small,
                        ),
                    contentScale = ContentScale.Crop,
                    enableLoadingIndicator = true,
                    enableErrorIndicator = true,
                )
                Text(
                    text = item.name,
                    modifier = Modifier.padding(
                        start = 4.dp,
                        top = 4.dp,
                        end = 4.dp,
                        bottom = 0.dp,
                    ),
                    color = colorScheme.onBackground,
                    fontSize = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                Text(
                    text = item.size.toString(),
                    modifier = Modifier.padding(
                        start = 4.dp,
                        top = 0.dp,
                        end = 4.dp,
                        bottom = 4.dp,
                    ),
                    color = colorScheme.onBackground,
                    fontSize = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            }
        }
    }
}

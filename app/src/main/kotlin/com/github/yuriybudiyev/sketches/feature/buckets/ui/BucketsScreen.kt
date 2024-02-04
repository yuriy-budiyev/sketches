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

package com.github.yuriybudiyev.sketches.feature.buckets.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesLazyVerticalGrid
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.dimen.SketchesDimens
import com.github.yuriybudiyev.sketches.feature.buckets.navigation.BucketsNavigationDestination

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
    BucketsScreen(
        uiState = uiState,
        coroutineScope = coroutineScope,
        onBucketClick = onBucketClick,
    )
}

@Composable
fun BucketsScreen(
    uiState: BucketsScreenUiState,
    coroutineScope: CoroutineScope,
    onBucketClick: (index: Int, bucket: MediaStoreBucket) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            BucketsScreenUiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(id = R.string.no_buckets_found),
                    modifier = Modifier.matchParentSize(),
                )
            }
            BucketsScreenUiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is BucketsScreenUiState.Buckets -> {
                BucketsScreenLayout(
                    buckets = uiState.buckets,
                    onBucketClick = onBucketClick,
                    coroutineScope = coroutineScope,
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
            text = stringResource(id = BucketsNavigationDestination.titleRes),
            backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun BucketsScreenLayout(
    buckets: List<MediaStoreBucket>,
    onBucketClick: (index: Int, bucket: MediaStoreBucket) -> Unit,
    coroutineScope: CoroutineScope,
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
                        coroutineScope.launch {
                            onBucketClickUpdated(
                                index,
                                item,
                            )
                        }
                    }
                    .semantics {
                        testTag = "bucket"
                    },
            ) {
                SketchesAsyncImage(
                    uri = item.coverUri,
                    description = stringResource(id = R.string.bucket_cover),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio = 1f)
                        .clip(shape = MaterialTheme.shapes.small)
                        .border(
                            width = SketchesDimens.MediaItemBorderThickness,
                            color = MaterialTheme.colorScheme.onBackground,
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
                    color = MaterialTheme.colorScheme.onBackground,
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
                    color = MaterialTheme.colorScheme.onBackground,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

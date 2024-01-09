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
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.data.model.MediaType
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.component.media.SketchesMediaPlayer
import com.github.yuriybudiyev.sketches.core.ui.component.media.rememberSketchesMediaState
import com.github.yuriybudiyev.sketches.core.ui.effect.LifecycleEventEffect

@Composable
fun ImageRoute(
    fileIndex: Int,
    fileId: Long,
    bucketId: Long,
    viewModel: ImageScreenViewModel = hiltViewModel(),
    onShare: (uri: Uri, type: String) -> Unit,
) {
    com.github.yuriybudiyev.sketches.feature.image.ui.old.ImageRoute(
        fileIndex = fileIndex,
        fileId = fileId,
        bucketId = bucketId,
        viewModel = viewModel,
        onShare = onShare,
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FilePager(
    index: Int,
    files: List<MediaStoreFile>,
    onPageChanged: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val indexUpdated by rememberUpdatedState(index)
    val filesUpdated by rememberUpdatedState(files)
    val onPageChangedUpdated by rememberUpdatedState(onPageChanged)
    var currentPage by remember(indexUpdated) { mutableIntStateOf(indexUpdated) }
    val pagerState = rememberPagerState(indexUpdated) { filesUpdated.size }
    val pagerScope = rememberCoroutineScope()
    LaunchedEffect(
        pagerState,
        pagerScope
    ) {
        snapshotFlow { indexUpdated }.collect { page ->
            pagerScope.launch {
                pagerState.scrollToPage(page)
            }
        }
    }
    LaunchedEffect(
        pagerState,
        pagerScope
    ) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            currentPage = page
            val file = filesUpdated[page]
            pagerScope.launch {
                onPageChangedUpdated(
                    page,
                    file
                )
            }
        }
    }
    HorizontalPager(
        state = pagerState,
        key = { page -> filesUpdated[page].id },
        modifier = modifier,
    ) { page ->
        val file = filesUpdated[page]
        FilePage(
            fileUri = file.uri,
            fileType = file.mediaType,
            isCurrentPage = page == currentPage,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun FilePage(
    fileUri: Uri,
    fileType: MediaType,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier,
) {
    when (fileType) {
        MediaType.IMAGE -> {
            ImagePage(
                fileUri = fileUri,
                modifier = modifier,
            )
        }
        MediaType.VIDEO -> {
            VideoPage(
                fileUri = fileUri,
                isCurrentPage = isCurrentPage,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ImagePage(
    fileUri: Uri,
    modifier: Modifier = Modifier,
) {
    SketchesAsyncImage(
        uri = fileUri,
        description = stringResource(id = R.string.image),
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.High,
        modifier = modifier,
    )
}

@Composable
private fun VideoPage(
    fileUri: Uri,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier,
) {
    val fileUriUpdated by rememberUpdatedState(fileUri)
    val isCurrentPageUpdated by rememberUpdatedState(isCurrentPage)
    val mediaState = rememberSketchesMediaState()
    val mediaScope = rememberCoroutineScope()
    LaunchedEffect(
        mediaState,
        mediaScope
    ) {
        snapshotFlow { fileUriUpdated }.collect { uri ->
            if (mediaState.uri != uri) {
                mediaScope.launch {
                    mediaState.open(uri)
                }
            }
        }
    }
    LaunchedEffect(
        mediaState,
        mediaScope
    ) {
        snapshotFlow { isCurrentPageUpdated }.collect { currentPage ->
            if (currentPage) {
                mediaScope.launch {
                    mediaState.disableVolume()
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
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        mediaScope.launch {
            mediaState.pause()
        }
    }
    SketchesMediaPlayer(
        state = mediaState,
        modifier = modifier,
    )
}

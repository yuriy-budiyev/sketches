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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaType
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.component.media.SketchesMediaPlayer
import com.github.yuriybudiyev.sketches.core.ui.component.media.rememberSketchesMediaState

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
    val mediaState = rememberSketchesMediaState()
    val mediaScope = rememberCoroutineScope()
    LaunchedEffect(
        mediaState,
        mediaScope,
        fileUri,
    ) {
        if (mediaState.uri != fileUri) {
            mediaScope.launch {
                mediaState.open(fileUri)
            }
        }
    }
    LaunchedEffect(
        mediaState,
        mediaScope,
        isCurrentPage,
    ) {
        if (isCurrentPage) {
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
    SketchesMediaPlayer(
        state = mediaState,
        modifier = modifier,
    )
}

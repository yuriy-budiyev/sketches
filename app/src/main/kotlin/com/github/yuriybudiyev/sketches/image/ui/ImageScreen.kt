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

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
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
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.component.media.SketchesMediaPlayer
import com.github.yuriybudiyev.sketches.core.ui.component.media.rememberSketchesMediaState
import com.github.yuriybudiyev.sketches.core.ui.effect.LifecycleEventEffect
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons
import com.github.yuriybudiyev.sketches.images.data.model.MediaStoreFile

@Composable
fun ImageRoute(
    imageIndex: Int,
    imageId: Long,
    bucketId: Long,
    viewModel: ImageScreenViewModel = hiltViewModel(),
    onShare: (file: MediaStoreFile) -> Unit
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
        onShare = onShare
    )
}

@Composable
fun ImageScreen(
    uiState: ImageScreenUiState,
    onChange: (index: Int, file: MediaStoreFile) -> Unit,
    onShare: (file: MediaStoreFile) -> Unit
) {
    when (uiState) {
        ImageScreenUiState.Empty -> {
            SketchesCenteredMessage(text = stringResource(id = R.string.no_images_found))
        }
        ImageScreenUiState.Loading -> {
            SketchesLoadingIndicator()
        }
        is ImageScreenUiState.Image -> {
            ImageScreenLayout(
                index = uiState.imageIndex,
                files = uiState.images,
                onChange = onChange,
                onShare = onShare
            )
        }
        is ImageScreenUiState.Error -> {
            SketchesCenteredMessage(text = stringResource(id = R.string.unexpected_error))
        }
    }
}

@Composable
private fun ImageScreenLayout(
    index: Int,
    files: List<MediaStoreFile>,
    onChange: (index: Int, file: MediaStoreFile) -> Unit,
    onShare: (file: MediaStoreFile) -> Unit
) {
    var currentFile by remember { mutableStateOf(files[index]) }
    Box(modifier = Modifier.fillMaxSize()) {
        ImagePager(
            index = index,
            files = files,
            onChange = { index, file ->
                currentFile = file
                onChange(
                    index,
                    file
                )
            },
            modifier = Modifier.matchParentSize()
        )
        TopBar(
            onShareClick = { onShare(currentFile) },
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun TopBar(
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SketchesTopAppBar(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(modifier = Modifier
            .size(size = 48.dp)
            .clip(shape = CircleShape)
            .clickable(onClick = onShareClick),
            contentAlignment = Alignment.Center,
            content = {
                Icon(
                    imageVector = SketchesIcons.Share,
                    contentDescription = null,
                    modifier = Modifier.size(size = 24.dp)
                )
            })
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ImagePager(
    index: Int,
    files: List<MediaStoreFile>,
    onChange: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val data by rememberUpdatedState(files)
    val state = rememberPagerState(index) { data.size }
    LaunchedEffect(state) {
        snapshotFlow { state.currentPage }.collect { page ->
            onChange(
                page,
                data[page]
            )
        }
    }
    LaunchedEffect(index) {
        snapshotFlow { index }.collect { index ->
            state.scrollToPage(index)
        }
    }
    HorizontalPager(state = state,
        modifier = modifier,
        key = { page -> data[page].id },
        pageContent = { page ->
            ImagePage(
                file = data[page],
                modifier = Modifier.fillMaxSize()
            )
        })
}

@Composable
private fun ImagePage(
    file: MediaStoreFile,
    modifier: Modifier = Modifier
) {
    when (file.type) {
        MediaStoreFile.Type.IMAGE -> {
            ImageLayout(
                uri = file.uri,
                modifier = modifier
            )
        }
        MediaStoreFile.Type.VIDEO -> {
            VideoLayout(
                uri = file.uri,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ImageLayout(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    SketchesAsyncImage(
        uri = uri,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.High
    )
}

@Composable
private fun VideoLayout(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val state = rememberSketchesMediaState()
    SketchesMediaPlayer(
        state = state,
        modifier = modifier
    )
    LaunchedEffect(uri) {
        if (state.uri != uri) {
            state.open(uri)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        if (state.isPlaying) {
            state.pause()
        }
    }
}

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

package com.github.yuriybudiyev.sketches.feature.images.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesAppBarActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesCenteredMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesErrorMessage
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLoadingIndicator
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesMediaVerticalGrid
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesTopAppBar
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import com.github.yuriybudiyev.sketches.feature.images.navigation.ImagesNavigationDestination
import kotlinx.coroutines.launch

@Composable
fun ImagesRoute(
    onImageClick: (index: Int, file: MediaStoreFile) -> Unit,
    onRequestUserSelectedMedia: (() -> Unit)? = null,
    viewModel: ImagesScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(
        viewModel,
        coroutineScope,
    ) {
        coroutineScope.launch {
            viewModel.updateMedia()
        }
    }
    var mediaAccessRequested by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (mediaAccessRequested) {
            mediaAccessRequested = false
            coroutineScope.launch {
                viewModel.updateMedia()
            }
        }
    }
    ImagesScreen(
        uiState = uiState,
        onRequestUserSelectedMedia = if (onRequestUserSelectedMedia != null) {
            {
                mediaAccessRequested = true
                onRequestUserSelectedMedia()
            }
        } else {
            null
        },
        onImageClick = onImageClick,
    )
}

@Composable
fun ImagesScreen(
    uiState: ImagesScreenUiState,
    onRequestUserSelectedMedia: (() -> Unit)?,
    onImageClick: (index: Int, file: MediaStoreFile) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            ImagesScreenUiState.Empty -> {
                SketchesCenteredMessage(
                    text = stringResource(id = R.string.no_images_found),
                    modifier = Modifier.matchParentSize(),
                )
            }
            ImagesScreenUiState.Loading -> {
                SketchesLoadingIndicator(modifier = Modifier.matchParentSize())
            }
            is ImagesScreenUiState.Images -> {
                SketchesMediaVerticalGrid(
                    files = uiState.files,
                    onItemClick = onImageClick,
                    modifier = Modifier.matchParentSize(),
                    overlayTop = true,
                    overlayBottom = true,
                )
            }
            is ImagesScreenUiState.Error -> {
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
            text = stringResource(id = ImagesNavigationDestination.titleRes),
            backgroundColor = MaterialTheme.colorScheme.background.copy(alpha = SketchesColors.UiAlphaHigh),
            actions = {
                if (onRequestUserSelectedMedia != null) {
                    SketchesAppBarActionButton(
                        icon = SketchesIcons.UpdateMediaSelection,
                        description = stringResource(id = R.string.update_selected_media),
                        onClick = {
                            onRequestUserSelectedMedia()
                        },
                    )
                }
            },
        )
    }
}

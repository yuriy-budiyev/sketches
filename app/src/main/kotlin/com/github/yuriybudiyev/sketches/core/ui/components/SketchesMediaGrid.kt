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

package com.github.yuriybudiyev.sketches.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons

@Composable
fun SketchesMediaGrid(
    files: List<MediaStoreFile>,
    selectedFiles: SnapshotStateSet<MediaStoreFile>,
    onItemClick: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
) {
    val filesUpdated by rememberUpdatedState(files)
    val selectedFilesUpdated by rememberUpdatedState(selectedFiles)
    val onItemClickUpdated by rememberUpdatedState(onItemClick)
    SketchesLazyGrid(
        modifier = modifier,
        state = state,
        overlayTop = overlayTop,
        overlayBottom = overlayBottom,
    ) {
        items(
            count = filesUpdated.size,
            key = { index -> filesUpdated[index].id },
            contentType = { index -> filesUpdated[index].mediaType },
        ) { index ->
            val file = filesUpdated[index]
            val fileSelectedOnComposition = selectedFilesUpdated.contains(file)
            Box(
                modifier = Modifier
                    .aspectRatio(ratio = 1.0F)
                    .border(
                        width = SketchesDimens.MediaItemBorderThickness,
                        color = if (fileSelectedOnComposition) {
                            MaterialTheme.colorScheme.onBackground
                                .copy(alpha = SketchesColors.UiAlphaLowTransparency)
                        } else {
                            MaterialTheme.colorScheme.onBackground
                                .copy(alpha = SketchesColors.UiAlphaHighTransparency)
                        },
                        shape = MaterialTheme.shapes.extraSmall,
                    )
                    .clip(shape = MaterialTheme.shapes.extraSmall)
                    .combinedClickable(
                        onLongClick = {
                            if (selectedFilesUpdated.isEmpty()) {
                                selectedFilesUpdated.add(file)
                            } else {
                                if (selectedFilesUpdated.contains(file)) {
                                    selectedFilesUpdated.clear()
                                } else {
                                    selectedFilesUpdated.addAll(filesUpdated)
                                }
                            }
                        },
                        onClick = {
                            if (selectedFilesUpdated.isNotEmpty()) {
                                if (selectedFilesUpdated.contains(file)) {
                                    selectedFilesUpdated.remove(file)
                                } else {
                                    selectedFilesUpdated.add(file)
                                }
                            } else {
                                onItemClickUpdated(
                                    index,
                                    file,
                                )
                            }
                        },
                    ),
            ) {
                SketchesAsyncImage(
                    uri = file.uri,
                    contentDescription = stringResource(
                        id = when (file.mediaType) {
                            MediaType.Image -> R.string.image
                            MediaType.Video -> R.string.video
                        },
                    ),
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Low,
                    enableLoadingIndicator = true,
                    enableErrorIndicator = true,
                )
                if (fileSelectedOnComposition) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                color = MaterialTheme.colorScheme.background
                                    .copy(alpha = SketchesColors.UiAlphaHighTransparency),
                            ),
                    )
                }
                if (file.mediaType == MediaType.Video) {
                    Box(
                        modifier = Modifier
                            .align(alignment = Alignment.BottomStart)
                            .padding(all = SketchesDimens.MediaGridVideoIconPadding)
                            .let { modifier ->
                                if (!fileSelectedOnComposition) {
                                    modifier.background(
                                        color = MaterialTheme.colorScheme.background
                                            .copy(alpha = SketchesColors.UiAlphaHighTransparency),
                                        shape = CircleShape,
                                    )
                                } else {
                                    modifier
                                }
                            },
                    ) {
                        Icon(
                            imageVector = SketchesIcons.Video,
                            contentDescription = stringResource(R.string.video),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        }
    }
}

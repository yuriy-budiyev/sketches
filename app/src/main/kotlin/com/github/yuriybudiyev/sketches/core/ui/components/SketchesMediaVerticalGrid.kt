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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens

@Composable
fun SketchesMediaVerticalGrid(
    files: List<MediaStoreFile>,
    onItemClick: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier,
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
) {
    val filesUpdated by rememberUpdatedState(files)
    val onItemClickUpdated by rememberUpdatedState(onItemClick)
    SketchesLazyVerticalGrid(
        overlayTop = overlayTop,
        overlayBottom = overlayBottom,
        modifier = modifier,
    ) {
        items(
            count = filesUpdated.size,
            key = { index -> filesUpdated[index].id },
            contentType = { index -> filesUpdated[index].mediaType },
        ) { index ->
            val file = filesUpdated[index]
            SketchesMediaItem(
                uri = file.uri,
                type = file.mediaType,
                videoIconPadding = SketchesDimens.LazyGridVideoIconPadding,
                modifier = Modifier
                    .aspectRatio(ratio = 1.0F)
                    .clip(shape = MaterialTheme.shapes.small)
                    .border(
                        width = SketchesDimens.MediaItemBorderThickness,
                        color = MaterialTheme.colorScheme.onBackground
                            .copy(alpha = SketchesColors.UiAlphaHighTransparency),
                        shape = MaterialTheme.shapes.small
                    )
                    .clickable {
                        onItemClickUpdated(
                            index,
                            file
                        )
                    },
            )
        }
    }
}

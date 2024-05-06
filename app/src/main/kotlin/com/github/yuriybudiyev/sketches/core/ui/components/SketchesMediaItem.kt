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

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaType
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons

@Composable
fun SketchesMediaItem(
    uri: Uri,
    type: MediaType,
    iconPadding: Dp,
    modifier: Modifier = Modifier,
) {
    when (type) {
        MediaType.Image -> {
            SketchesAsyncImage(
                uri = uri,
                contentDescription = stringResource(id = R.string.image),
                modifier = modifier,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low,
                enableLoadingIndicator = true,
                enableErrorIndicator = true,
            )
        }
        MediaType.Video -> {
            Box(modifier = modifier) {
                SketchesAsyncImage(
                    uri = uri,
                    contentDescription = stringResource(id = R.string.image),
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Low,
                    enableLoadingIndicator = true,
                    enableErrorIndicator = true,
                )
                Box(
                    modifier = Modifier
                        .align(alignment = Alignment.BottomEnd)
                        .padding(all = iconPadding)
                        .background(
                            color = MaterialTheme.colorScheme.background.copy(alpha = SketchesColors.UiAlphaLowTransparency),
                            shape = CircleShape
                        ),
                ) {
                    Icon(
                        imageVector = SketchesIcons.Video,
                        contentDescription = stringResource(id = R.string.video),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

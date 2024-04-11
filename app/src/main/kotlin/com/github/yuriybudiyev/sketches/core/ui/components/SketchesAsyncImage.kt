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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.SubcomposeAsyncImage
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons

@Composable
fun SketchesAsyncImage(
    uri: Uri,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    filterQuality: FilterQuality = FilterQuality.Low,
    enableLoadingIndicator: Boolean = true,
    enableErrorIndicator: Boolean = true,
) {
    SubcomposeAsyncImage(
        model = uri,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        filterQuality = filterQuality,
        loading = {
            if (enableLoadingIndicator) {
                StateIcon(
                    icon = SketchesIcons.ImageLoading,
                    description = stringResource(id = R.string.image_loading),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
        error = {
            if (enableErrorIndicator) {
                StateIcon(
                    icon = SketchesIcons.ImageError,
                    description = stringResource(id = R.string.image_error),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}

@Composable
private fun StateIcon(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(SketchesDimens.AsyncImageStateIconSize),
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

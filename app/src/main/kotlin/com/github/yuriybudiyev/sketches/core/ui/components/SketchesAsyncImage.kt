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

import androidx.annotation.FloatRange
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import coil3.size.Size
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons

@Composable
fun SketchesAsyncImage(
    uri: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    filterQuality: FilterQuality = FilterQuality.Low,
    enableLoadingIndicator: Boolean = true,
    enableErrorIndicator: Boolean = true,
) {
    val context = LocalPlatformContext.current
    val sizeResolver = rememberConstraintsSizeResolver()
    val request = remember(
        context,
        sizeResolver,
        uri,
    ) {
        ImageRequest.Builder(context)
            .size(sizeResolver)
            .data(uri)
            .build()
    }
    var painterState by remember {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val painter = rememberAsyncImagePainter(
        model = request,
        onState = { state ->
            painterState = state
        },
        contentScale = contentScale,
        filterQuality = filterQuality
    )
    Box(
        modifier = modifier.then(sizeResolver),
        contentAlignment = Alignment.Center,
    ) {
        when (painterState) {
            is AsyncImagePainter.State.Loading -> {
                if (enableLoadingIndicator) {
                    SketchesStateIconInternal(
                        icon = SketchesIcons.ImageLoading,
                        description = stringResource(R.string.image_loading),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            is AsyncImagePainter.State.Error -> {
                if (enableErrorIndicator) {
                    SketchesStateIconInternal(
                        icon = SketchesIcons.ImageError,
                        description = stringResource(R.string.image_error),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            is AsyncImagePainter.State.Success -> {
                Box(
                    modifier = Modifier
                        .semantics {
                            this.contentDescription = contentDescription
                            this.role = Role.Image
                        }
                        .align(Alignment.Center)
                        .matchParentSize()
                        .clipToBounds()
                        .paint(
                            painter,
                            contentScale = contentScale,
                            alignment = Alignment.Center,
                        )
                )
            }
            else -> {
                // Do nothing
            }
        }
    }
}

@Composable
fun SketchesZoomableAsyncImage(
    uri: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    @FloatRange(
        from = 1.0,
        fromInclusive = true,
    )
    maxRelativeZoom: Float = 10f,
    @FloatRange(
        from = 0.0,
        fromInclusive = true,
        to = 1.0,
        toInclusive = true,
    )
    doubleTapZoomFraction: Float = 0.2f,
    enableLoadingIndicator: Boolean = true,
    enableErrorIndicator: Boolean = true,
) {
    var painterState by remember {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val painter = rememberAsyncImagePainter(
        model = ImageRequest
            .Builder(LocalPlatformContext.current)
            .size(Size.ORIGINAL)
            .data(uri)
            .build(),
        onState = { state ->
            painterState = state
        },
        contentScale = ContentScale.None,
        filterQuality = FilterQuality.High
    )
    when (painterState) {
        is AsyncImagePainter.State.Loading -> {
            if (enableLoadingIndicator) {
                SketchesStateIconInternal(
                    icon = SketchesIcons.ImageLoading,
                    description = stringResource(R.string.image_loading),
                    modifier = modifier,
                )
            }
        }
        is AsyncImagePainter.State.Error -> {
            if (enableErrorIndicator) {
                SketchesStateIconInternal(
                    icon = SketchesIcons.ImageError,
                    description = stringResource(R.string.image_error),
                    modifier = modifier,
                )
            }
        }
        is AsyncImagePainter.State.Success -> {
            SketchesZoomableBox(
                modifier = modifier.semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                    this.role = Role.Image
                },
                onTap = onTap,
                maxRelativeZoom = maxRelativeZoom,
                doubleTapZoomFraction = doubleTapZoomFraction
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .wrapContentSize(
                            align = Alignment.Center,
                            unbounded = true,
                        )
                        .zoomable()
                        .paint(
                            painter,
                            contentScale = ContentScale.None,
                            alignment = Alignment.Center,
                        ),
                )
            }
        }
        else -> {
            Box(modifier)
        }
    }
}

@Composable
private fun SketchesStateIconInternal(
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

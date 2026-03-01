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
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import coil3.video.videoFramePercent
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens

@Composable
fun SketchesAsyncImage(
    uri: Uri,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    filterQuality: FilterQuality = FilterQuality.Low,
    enableImageStateBackground: Boolean = true,
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
            .videoFramePercent(0.1)
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
        filterQuality = filterQuality,
    )
    Box(
        modifier = Modifier
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
            .then(modifier)
            .then(sizeResolver),
    ) {
        when (painterState) {
            is AsyncImagePainter.State.Empty -> {
                // Do nothing
            }
            is AsyncImagePainter.State.Loading -> {
                if (enableImageStateBackground) {
                    SketchesImageStateBackground(modifier = Modifier.matchParentSize())
                }
                if (enableLoadingIndicator) {
                    SketchesLoadingStateIcon(modifier = Modifier.align(Alignment.Center))
                }
            }
            is AsyncImagePainter.State.Error -> {
                if (enableImageStateBackground) {
                    SketchesImageStateBackground(modifier = Modifier.matchParentSize())
                }
                if (enableErrorIndicator) {
                    SketchesErrorStateIcon(modifier = Modifier.align(Alignment.Center))
                }
            }
            is AsyncImagePainter.State.Success -> {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clipToBounds()
                        .paint(
                            painter,
                            contentScale = contentScale,
                            alignment = Alignment.Center,
                        ),
                )
            }
        }
    }
}

@Composable
fun SketchesZoomableAsyncImage(
    uri: Uri,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    enableImageStateBackground: Boolean = true,
    enableLoadingIndicator: Boolean = true,
    enableErrorIndicator: Boolean = true,
) {
    val context = LocalPlatformContext.current
    val request = remember(
        context,
        uri,
    ) {
        ImageRequest.Builder(context)
            .videoFramePercent(0.1)
            .size(Size.ORIGINAL)
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
        contentScale = ContentScale.None,
        filterQuality = FilterQuality.High,
    )
    Box(
        modifier = Modifier
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
            .then(modifier),
    ) {
        when (painterState) {
            is AsyncImagePainter.State.Empty -> {
                // Do nothing
            }
            is AsyncImagePainter.State.Loading -> {
                if (enableImageStateBackground) {
                    SketchesImageStateBackground(modifier = Modifier.matchParentSize())
                }
                if (enableLoadingIndicator) {
                    SketchesLoadingStateIcon(modifier = Modifier.align(Alignment.Center))
                }
            }
            is AsyncImagePainter.State.Error -> {
                if (enableImageStateBackground) {
                    SketchesImageStateBackground(modifier = Modifier.matchParentSize())
                }
                if (enableErrorIndicator) {
                    SketchesErrorStateIcon(modifier = Modifier.align(Alignment.Center))
                }
            }
            is AsyncImagePainter.State.Success -> {
                SketchesZoomableBox(
                    modifier = Modifier.matchParentSize(),
                    onTap = onTap,
                ) {
                    Box(
                        modifier = Modifier
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
        }
    }
}

@Composable
@NonRestartableComposable
private fun SketchesImageStateBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.onBackground
                    .copy(alpha = SketchesColors.UiAlphaHighTransparency),
                shape = RectangleShape,
            ),
    )
}

@Composable
@NonRestartableComposable
private fun SketchesLoadingStateIcon(modifier: Modifier = Modifier) {
    SketchesStateIcon(
        iconRes = R.drawable.ic_image_loading,
        contentDescription = stringResource(R.string.image_loading),
        modifier = modifier,
    )
}

@Composable
@NonRestartableComposable
private fun SketchesErrorStateIcon(modifier: Modifier = Modifier) {
    SketchesStateIcon(
        iconRes = R.drawable.ic_image_error,
        contentDescription = stringResource(R.string.image_error),
        modifier = modifier,
    )
}

@Composable
private fun SketchesStateIcon(
    @DrawableRes
    iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        modifier = Modifier
            .size(SketchesDimens.AsyncImageStateIconSize)
            .then(modifier),
        tint = MaterialTheme.colorScheme.onBackground,
    )
}

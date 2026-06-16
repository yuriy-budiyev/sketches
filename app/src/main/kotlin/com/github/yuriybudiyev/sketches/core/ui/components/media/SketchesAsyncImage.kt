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

package com.github.yuriybudiyev.sketches.core.ui.components.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesZoomableBox
import com.github.yuriybudiyev.sketches.core.ui.components.media.cache.SketchesMemoryCacheKeys
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens

@Composable
fun SketchesThumbnailAsyncImage(
    uri: Uri,
    memoryCacheKey: MemoryCache.Key,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalPlatformContext.current
    val sizeResolver = rememberConstraintsSizeResolver()
    val request = remember(
        uri,
        memoryCacheKey,
        context,
        sizeResolver,
    ) {
        ImageRequest.Builder(context)
            .memoryCacheKey(memoryCacheKey)
            .data(uri)
            .size(sizeResolver)
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
        contentScale = ContentScale.Crop,
        filterQuality = FilterQuality.Low,
    )
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
            .background(
                color = colorScheme.onBackground
                    .copy(alpha = SketchesColors.UiAlphaHighTransparency),
                shape = RectangleShape,
            )
            .then(modifier)
            .then(sizeResolver),
    ) {
        if (painterState is AsyncImagePainter.State.Success) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clipToBounds()
                    .paint(
                        painter = painter,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                    ),
            )
        } else if (painterState is AsyncImagePainter.State.Error) {
            Icon(
                painter = painterResource(R.drawable.ic_image_error),
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(LocalDimens.current.asyncImageStateIconSize)
                    .align(Alignment.Center),
                tint = colorScheme.onBackground,
            )
        }
    }
}

@Composable
fun SketchesPreviewAsyncImage(
    uri: Uri,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val context = LocalPlatformContext.current
    val request = remember(
        uri,
        context,
    ) {
        ImageRequest
            .Builder(context)
            .placeholderMemoryCacheKey(SketchesMemoryCacheKeys.thumbnail(uri))
            .memoryCacheKey(SketchesMemoryCacheKeys.preview(uri))
            .data(uri)
            .size(coil3.size.Size.ORIGINAL)
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
        contentAlignment = Alignment.Center,
    ) {
        when (painterState) {
            is AsyncImagePainter.State.Empty -> {
                // Do nothing
            }
            is AsyncImagePainter.State.Loading,
            is AsyncImagePainter.State.Success -> {
                SketchesZoomableBox(
                    modifier = Modifier.matchParentSize(),
                    onTap = onTap,
                ) {
                    val size = painter.intrinsicSize
                    if (size != Size.Zero && size != Size.Unspecified) {
                        Box(
                            modifier = Modifier
                                .zoomable()
                                .aspectRatio(
                                    ratio = size.width / size.height,
                                    matchHeightConstraintsFirst = false,
                                )
                                .paint(
                                    painter = painter,
                                    contentScale = ContentScale.Fit,
                                    alignment = Alignment.Center,
                                ),
                        )
                    }
                }
            }
            is AsyncImagePainter.State.Error -> {
                Icon(
                    painter = painterResource(R.drawable.ic_image_error),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .size(LocalDimens.current.asyncImageStateIconSize)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

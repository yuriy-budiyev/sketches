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

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import coil3.BitmapImage
import coil3.Image
import coil3.compose.AsyncImagePainter
import coil3.compose.asPainter
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Scale
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.imageloader.cache.allowLocalCacheIntercept
import com.github.yuriybudiyev.sketches.core.imageloader.cache.imageMemoryCache
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultAnimationSpec
import com.github.yuriybudiyev.sketches.core.ui.colors.withHighTransparency
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesZoomableBox
import com.github.yuriybudiyev.sketches.core.ui.components.ZoomState
import com.github.yuriybudiyev.sketches.core.ui.components.rememberZoomState
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens

@Composable
fun SketchesThumbnailAsyncImage(
    uri: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sizeResolver = rememberConstraintsSizeResolver()
    val request = remember(
        uri,
        context,
        sizeResolver,
    ) {
        ImageRequest.Builder(context)
            .data(uri)
            .size(sizeResolver)
            .scale(Scale.FILL)
            .allowLocalCacheIntercept(true)
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
        filterQuality = FilterQuality.High,
    )
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
            .background(
                color = colorScheme.onBackground.withHighTransparency(),
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
                modifier = Modifier.align(Alignment.Center),
                tint = colorScheme.onBackground,
            )
        }
    }
}

@Composable
fun SketchesPreviewAsyncImage(
    uri: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    zoomState: ZoomState = rememberZoomState(),
    onTap: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val request = remember(
        uri,
        context,
    ) {
        ImageRequest
            .Builder(context)
            .data(uri)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .size(coil3.size.Size.ORIGINAL)
            .allowLocalCacheIntercept(false)
            .build()
    }
    var thumbnailVisible by remember { mutableStateOf(true) }
    var errorVisible by remember { mutableStateOf(false) }
    val imageAlpha = remember { Animatable(0F).apply { updateBounds(0F, 1F) } }
    var imagePainter by remember { mutableStateOf<Painter?>(null) }
    var image by remember { mutableStateOf<Image?>(null) }
    LaunchedEffect(request, context) {
        thumbnailVisible = true
        errorVisible = false
        imageAlpha.snapTo(0F)
        imagePainter = null
        image = null
        val imageResult = context.imageLoader.execute(request)
        when (imageResult) {
            is SuccessResult -> {
                errorVisible = false
                val resultImage = imageResult.image
                image = resultImage
                imagePainter = resultImage.asPainter(context, FilterQuality.High)
                imageAlpha.animateTo(1F, defaultAnimationSpec())
                thumbnailVisible = false
            }
            is ErrorResult -> {
                errorVisible = true
                thumbnailVisible = false
                imageAlpha.snapTo(0F)
                imagePainter = null
                image = null
            }
        }
    }
    DisposableEffect(image) {
        val image = image
        onDispose {
            if (image is BitmapImage) {
                val bitmap = image.bitmap
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailVisible) {
            val thumbnailPainter = remember(uri, context) {
                context.imageMemoryCache[uri]?.asPainter(context, FilterQuality.High)
            }
            if (thumbnailPainter != null) {
                val size = thumbnailPainter.intrinsicSize
                if (size != Size.Zero && size != Size.Unspecified) {
                    val ratio = size.width / size.height
                    Box(
                        modifier = Modifier
                            .aspectRatio(
                                ratio = ratio,
                                matchHeightConstraintsFirst = ratio < 1F,
                            )
                            .blur(
                                radius = LocalDimens.current.placeholderBlurRadius,
                                edgeTreatment = BlurredEdgeTreatment.Rectangle,
                            )
                            .paint(
                                painter = thumbnailPainter,
                                alignment = Alignment.Center,
                                contentScale = ContentScale.Fit,
                            ),
                    )
                }
            }
        }
        SketchesZoomableBox(
            modifier = Modifier
                .graphicsLayer {
                    alpha = imageAlpha.value
                }
                .matchParentSize(),
            zoomState = zoomState,
            onTap = onTap,
        ) {
            val imagePainter = imagePainter
            if (imagePainter != null) {
                val size = imagePainter.intrinsicSize
                if (size != Size.Zero && size != Size.Unspecified) {
                    val ratio = size.width / size.height
                    Box(
                        modifier = Modifier
                            .zoomable()
                            .aspectRatio(
                                ratio = ratio,
                                matchHeightConstraintsFirst = ratio < 1F,
                            )
                            .paint(
                                painter = imagePainter,
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.Center,
                            ),
                    )
                }
            }
        }
        if (errorVisible) {
            Icon(
                painter = painterResource(R.drawable.ic_image_error),
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

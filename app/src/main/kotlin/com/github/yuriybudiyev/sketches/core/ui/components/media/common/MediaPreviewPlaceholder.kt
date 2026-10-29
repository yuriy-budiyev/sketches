/*
 * MIT License
 *
 * Copyright (c) 2026 Yuriy Budiyev
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

package com.github.yuriybudiyev.sketches.core.ui.components.media.common

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.DeprecatedSinceApi
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.BitmapImage
import coil3.compose.asPainter
import com.github.yuriybudiyev.sketches.core.imageloader.cache.imageMemoryCache
import com.github.yuriybudiyev.sketches.core.media.createBlurredBitmap
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

@Composable
fun MediaPreviewPlaceholder(
    uri: String?,
    modifier: Modifier = Modifier,
) {
    if (uri == null) {
        return
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaPreviewPlaceholderImpl31(uri, modifier)
    } else {
        MediaPreviewPlaceholderImpl(uri, modifier)
    }

}

@Composable
@RequiresApi(Build.VERSION_CODES.S)
private fun MediaPreviewPlaceholderImpl31(
    uri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val painter = remember(uri, context) {
        context.imageMemoryCache[uri]?.asPainter(context, FilterQuality.High)
    }
    if (painter == null) {
        return
    }
    val size = painter.intrinsicSize
    if (size != Size.Zero && size != Size.Unspecified) {
        val ratio = size.width / size.height
        Box(
            modifier = modifier
                .aspectRatio(
                    ratio = ratio,
                    matchHeightConstraintsFirst = ratio < 1F,
                )
                .blur(
                    radius = LocalDimens.current.placeholderBlurRadius,
                    edgeTreatment = BlurredEdgeTreatment.Rectangle,
                )
                .paint(
                    painter = painter,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Fit,
                ),
        )
    }
}

@Composable
@DeprecatedSinceApi(Build.VERSION_CODES.S)
private fun MediaPreviewPlaceholderImpl(
    uri: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val image = remember(uri, context) { context.imageMemoryCache[uri] }
    if (image == null) {
        return
    }
    var painter by remember { mutableStateOf<Painter?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(image, context) {
        painter = null
        bitmap = null
        if (image is BitmapImage) {
            val imageBitmap = image.bitmap
            val blurred = withContext(Dispatchers.Default) {
                createBlurredBitmap(
                    context = context,
                    source = imageBitmap,
                    radius = (min(imageBitmap.width, imageBitmap.height).toFloat() * 0.0625F)
                        .coerceIn(1F, 25F),
                )
            }
            painter = BitmapPainter(blurred.asImageBitmap())
            bitmap = blurred
        } else {
            painter = image.asPainter(context)
            bitmap = null
        }
    }
    DisposableEffect(bitmap) {
        val currentBitmap = bitmap
        onDispose {
            if (currentBitmap != null && !currentBitmap.isRecycled) {
                currentBitmap.recycle()
            }
        }
    }
    val currentPainter = painter ?: return
    val size = currentPainter.intrinsicSize
    if (size != Size.Zero && size != Size.Unspecified) {
        val ratio = size.width / size.height
        Box(
            modifier = modifier
                .aspectRatio(
                    ratio = ratio,
                    matchHeightConstraintsFirst = ratio < 1F,
                )
                .paint(
                    painter = currentPainter,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Fit,
                ),
        )
    }
}

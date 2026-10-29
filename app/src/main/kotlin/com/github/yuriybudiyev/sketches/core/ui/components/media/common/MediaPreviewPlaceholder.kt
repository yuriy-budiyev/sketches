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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.asPainter
import com.github.yuriybudiyev.sketches.core.imageloader.cache.imageMemoryCache
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens

@Composable
fun MediaPreviewPlaceholder(
    uri: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val painter = remember(uri, context) {
        uri?.let { uri -> context.imageMemoryCache[uri]?.asPainter(context, FilterQuality.High) }
    }
    if (painter != null) {
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
}

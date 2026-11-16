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

package com.github.yuriybudiyev.sketches.core.ui.colors

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.debugInspectorInfo

const val LowTransparencyAlpha: Float = 0.75F
const val MediumTransparencyAlpha: Float = 0.35F
const val HighTransparencyAlpha: Float = 0.15F

@Stable
fun Color.withLowTransparency(): Color =
    copy(alpha = LowTransparencyAlpha)

@Stable
fun Color.withMediumTransparency(): Color =
    copy(alpha = MediumTransparencyAlpha)

@Stable
fun Color.withHighTransparency(): Color =
    copy(alpha = HighTransparencyAlpha)

@Stable
fun ColorScheme.topToBottomBackgroundGradientBrush(
    startY: Float = 0F,
    endY: Float = Float.POSITIVE_INFINITY,
): Brush =
    Brush.verticalGradient(
        colors = listOf(
            background,
            background.withLowTransparency(),
        ),
        startY = startY,
        endY = endY,
        tileMode = TileMode.Clamp,
    )

@Stable
fun ColorScheme.bottomToTopBackgroundGradientBrush(
    startY: Float = 0F,
    endY: Float = Float.POSITIVE_INFINITY,
): Brush =
    Brush.verticalGradient(
        colors = listOf(
            background.withLowTransparency(),
            background,
        ),
        startY = startY,
        endY = endY,
        tileMode = TileMode.Clamp,
    )

fun Modifier.verticalGradient(
    topColor: Color,
    bottomColor: Color,
    start: Float = 0F,
    end: Float = 1F,
): Modifier {
    require(start >= 0F) { "Start offset should be >= 0 ($start)" }
    require(end <= 1F) { "End offset should be <= 1 ($end)" }
    require(start < end) { "Start offset ($start) should be less than end offset ($end)" }
    return composed(
        debugInspectorInfo {
            name = "verticalGradient"
            properties["topColor"] = topColor
            properties["bottomColor"] = bottomColor
            properties["start"] = start
            properties["end"] = end
        },
    ) {
        val painter = remember(topColor, bottomColor, start, end) {
            val hasStart = start > 0F
            val hasEnd = end < 1F
            var rows = 1
            if (hasStart) rows++
            if (hasEnd) rows++
            return@remember MeshGradientPainter(rows = rows, columns = 1, hasBicubicColor = true) {
                var row = 0
                setVertex(row, 0, Offset(0F, 0F), topColor)
                setVertex(row, 1, Offset(1F, 0F), topColor)
                row++
                if (start > 0F) {
                    setVertex(row, 0, Offset(0F, start), topColor)
                    setVertex(row, 1, Offset(1F, start), topColor)
                    row++
                }
                if (end < 1F) {
                    setVertex(row, 0, Offset(0F, end), bottomColor)
                    setVertex(row, 1, Offset(1F, end), bottomColor)
                    row++
                }
                setVertex(row, 0, Offset(0F, 1F), bottomColor)
                setVertex(row, 1, Offset(1F, 1F), bottomColor)
            }
        }
        return@composed paint(
            painter = painter,
            sizeToIntrinsics = false,
            contentScale = ContentScale.FillBounds,
        )
    }
}

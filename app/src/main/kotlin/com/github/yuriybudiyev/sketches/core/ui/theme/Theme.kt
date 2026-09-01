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

package com.github.yuriybudiyev.sketches.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

const val LowTransparencyAlpha: Float = 0.75F
const val MediumTransparencyAlpha: Float = 0.35F
const val HighTransparencyAlpha: Float = 0.15F

@Stable
fun Color.withLowTransparency(): Color =
    this.copy(alpha = LowTransparencyAlpha)

@Stable
fun Color.withMediumTransparency(): Color =
    this.copy(alpha = MediumTransparencyAlpha)

@Stable
fun Color.withHighTransparency(): Color =
    this.copy(alpha = HighTransparencyAlpha)

@Stable
@Composable
fun rememberTopToBottomBackgroundGradientBrush(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    startY: Float = 0F,
    endY: Float = Float.POSITIVE_INFINITY,
): Brush =
    remember(colorScheme, startY, endY) {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.background,
                colorScheme.background.withLowTransparency(),
            ),
            startY = startY,
            endY = endY,
            tileMode = TileMode.Clamp,
        )
    }

@Stable
@Composable
fun rememberBottomToTopBackgroundGradientBrush(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    startY: Float = 0F,
    endY: Float = Float.POSITIVE_INFINITY,
): Brush =
    remember(colorScheme, startY, endY) {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.background.withLowTransparency(),
                colorScheme.background,
            ),
            startY = startY,
            endY = endY,
            tileMode = TileMode.Clamp,
        )
    }

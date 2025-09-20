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

package com.github.yuriybudiyev.sketches.core.ui.colors

import androidx.compose.ui.graphics.Color

object SketchesColors {

    object Light {

        val Primary = Color(0xFF000000)
        val OnPrimary = Color(0xFFEFEFEF)
        val Background = Color(0xFFEFEFEF)
        val OnBackground = Color(0xFF000000)
        val Surface = Color(0xFFEFEFEF)
        val OnSurface = Color(0xFF000000)
        val OnSurfaceVariant = Color(0xFF000000)
        val SurfaceContainerHigh = Color(0xFFE7E7E7)
    }

    object Dark {

        val Primary = Color(0xFFFFFFFF)
        val OnPrimary = Color(0xFF0F0F0F)
        val Background = Color(0xFF0F0F0F)
        val OnBackground = Color(0xFFFFFFFF)
        val Surface = Color(0xFF0F0F0F)
        val OnSurface = Color(0xFFFFFFFF)
        val OnSurfaceVariant = Color(0xFFFFFFFF)
        val SurfaceContainerHigh = Color(0xFF272727)
    }

    const val UiAlphaLowTransparency = 0.75F
    const val UiAlphaHighTransparency = 0.25F
}

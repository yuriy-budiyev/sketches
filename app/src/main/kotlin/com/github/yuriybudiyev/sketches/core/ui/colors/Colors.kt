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

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

object SketchesColors {

    @get:Stable
    val Primary = Color(0xFF6B69D6)

    @get:Stable
    val OnPrimary = Color(0xFFFFFFFF)

    @get:Stable
    val Shadow = Color(0xFF000000)

    object Light {

        @get:Stable
        val PrimaryContainer = Color(0x00000000)

        @get:Stable
        val OnPrimaryContainer = Color(0xFF000000)

        @get:Stable
        val Background = Color(0xFFFFFFFF)

        @get:Stable
        val OnBackground = Color(0xFF000000)

        @get:Stable
        val Surface = Color(0xFFFFFFFF)

        @get:Stable
        val OnSurface = Color(0xFF000000)

        @get:Stable
        val OnSurfaceVariant = Color(0xFF000000)

        @get:Stable
        val SurfaceContainerHigh = Color(0xFFF7F7F7)

        @get:Stable
        val Outline = Color(0xFF6B69D6)

        @get:Stable
        val OutlineVariant = Color(0xFF6B69D6)
    }

    object Dark {

        @get:Stable
        val PrimaryContainer = Color(0x00000000)

        @get:Stable
        val OnPrimaryContainer = Color(0xFFFFFFFF)

        @get:Stable
        val Background = Color(0xFF000000)

        @get:Stable
        val OnBackground = Color(0xFFFFFFFF)

        @get:Stable
        val Surface = Color(0xFF000000)

        @get:Stable
        val OnSurface = Color(0xFFFFFFFF)

        @get:Stable
        val OnSurfaceVariant = Color(0xFFFFFFFF)

        @get:Stable
        val SurfaceContainerHigh = Color(0xFF272727)

        @get:Stable
        val Outline = Color(0xFF6B69D6)

        @get:Stable
        val OutlineVariant = Color(0xFF6B69D6)
    }
}

@Stable
fun Color.withLowTransparency(): Color =
    this.copy(alpha = 0.85F)

@Stable
fun Color.withMediumTransparency(): Color =
    this.copy(alpha = 0.35F)

@Stable
fun Color.withHighTransparency(): Color =
    this.copy(alpha = 0.15F)

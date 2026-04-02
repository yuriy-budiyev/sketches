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

package com.github.yuriybudiyev.sketches.core.ui.dimens

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class SketchesDimens(
    val material3AppBarHeight: Dp = 64.dp,
    val bottomBarHeight: Dp = material3AppBarHeight,
    val lazyGridOverlayTop: Dp = material3AppBarHeight,
    val lazyGridOverlayBottom: Dp = bottomBarHeight,
    val lazyGridItemSize: Dp = 104.dp,
    val lazyGridItemSpacing: Dp = 4.dp,
    val mediaGridIconPadding: Dp = 4.dp,
    val mediaBarItemSize: Dp = 56.dp,
    val mediaItemBorderThickness: Dp = 1.dp,
    val mediaBarItemSpacing: Dp = 4.dp,
    val mediaBarVideoIconPadding: Dp = 2.dp,
    val asyncImageStateIconSize: Dp = 48.dp,
)

val LocalSketchesDimens: ProvidableCompositionLocal<SketchesDimens> =
    staticCompositionLocalOf { error("CompositionLocal LocalSketchesDimens not present") }

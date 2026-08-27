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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

@Immutable
interface Dimens {

    val material3AppBarHeight: Dp
    val material3NavBarHeight: Dp
    val material3NavRailWidth: Dp
    val material3NavBarIndicatorWidth: Dp
    val material3NavBarIndicatorHeight: Dp
    val lazyGridOverlayTop: Dp
    val lazyGridOverlayBottom: Dp
    val lazyGridItemSize: Dp
    val lazyGridItemSpacing: Dp
    val mediaBarSize: Dp
    val mediaGridIconPadding: Dp
    val mediaBarItemSize: Dp
    val mediaItemBorderThickness: Dp
    val mediaBarItemSpacing: Dp
    val mediaBarVideoIconPadding: Dp
    val placeholderBlurRadius: Dp
    val shadowBlurRadius: Dp
}

val LocalDimens: ProvidableCompositionLocal<Dimens> =
    staticCompositionLocalOf { error("CompositionLocal LocalDimens not present") }

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

package com.github.yuriybudiyev.sketches.main.ui.dimens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.yuriybudiyev.sketches.core.ui.dimens.Dimens

@Immutable
data class DefaultDimens(
    override val material3AppBarHeight: Dp = 64.dp,
    override val material3NavBarHeight: Dp = material3AppBarHeight,
    override val material3NavRailWidth: Dp = 72.dp,
    override val material3NavBarIndicatorWidth: Dp = 56.dp,
    override val material3NavBarIndicatorHeight: Dp = 32.dp,
    override val lazyGridOverlayTop: Dp = material3AppBarHeight,
    override val lazyGridOverlayBottom: Dp = material3NavBarHeight,
    override val lazyGridItemSize: Dp = 108.dp,
    override val lazyGridItemSpacing: Dp = 1.dp,
    override val mediaGridIconPadding: Dp = 4.dp,
    override val mediaBarSize: Dp = material3AppBarHeight,
    override val mediaBarItemSize: Dp = 56.dp,
    override val mediaItemBorderThickness: Dp = 1.dp,
    override val mediaBarItemSpacing: Dp = 1.dp,
    override val mediaBarVideoIconPadding: Dp = 2.dp,
    override val placeholderBlurRadius: Dp = 4.dp,
    override val shadowBlurRadius: Dp = 4.dp,
): Dimens

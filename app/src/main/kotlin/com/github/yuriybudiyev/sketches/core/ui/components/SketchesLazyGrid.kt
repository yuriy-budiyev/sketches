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

package com.github.yuriybudiyev.sketches.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens

@Composable
fun SketchesLazyGrid(
    modifier: Modifier = Modifier,
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
    content: LazyGridScope.() -> Unit,
) {
    val systemBarInsets = WindowInsets.systemBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val startBarPadding = systemBarInsets.calculateStartPadding(layoutDirection)
    val topBarPadding = systemBarInsets.calculateTopPadding()
    val endBarPadding = systemBarInsets.calculateEndPadding(layoutDirection)
    val bottomBarPadding = systemBarInsets.calculateBottomPadding()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = SketchesDimens.LazyGridItemSize),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = SketchesDimens.LazyGridItemSpacing + startBarPadding,
            top = if (overlayTop) {
                SketchesDimens.LazyGridOverlayHeight + topBarPadding
            } else {
                SketchesDimens.LazyGridItemSpacing + topBarPadding
            },
            end = SketchesDimens.LazyGridItemSpacing + endBarPadding,
            bottom = if (overlayBottom) {
                SketchesDimens.LazyGridOverlayHeight + bottomBarPadding
            } else {
                SketchesDimens.LazyGridItemSpacing + bottomBarPadding
            },
        ),
        horizontalArrangement = Arrangement.spacedBy(space = SketchesDimens.LazyGridItemSpacing),
        verticalArrangement = Arrangement.spacedBy(space = SketchesDimens.LazyGridItemSpacing),
        content = content
    )
}

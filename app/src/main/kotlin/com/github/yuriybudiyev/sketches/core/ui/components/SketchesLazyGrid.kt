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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.waterfall
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.coerceAtLeast
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun rememberSketchesLazyGridState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): LazyGridState =
    rememberLazyGridState(
        cacheWindow = LazyLayoutCacheWindow(
            aheadFraction = 0.5f,
            behindFraction = 0.5f,
        ),
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset,
    )

@Composable
fun SketchesLazyGrid(
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberSketchesLazyGridState(),
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
    content: LazyGridScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val contentPaddings = WindowInsets.systemBars
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
        .union(WindowInsets.waterfall.only(WindowInsetsSides.Horizontal))
        .asPaddingValues()
    val contentPaddingStart = contentPaddings
        .calculateStartPadding(layoutDirection)
        .coerceAtLeast(SketchesDimens.LazyGridItemSpacing)
    val contentPaddingTop = contentPaddings
        .calculateTopPadding()
        .plus(
            if (overlayTop) {
                SketchesDimens.LazyGridOverlayTop
            } else {
                SketchesDimens.LazyGridItemSpacing
            },
        )
    val contentPaddingEnd = contentPaddings
        .calculateEndPadding(layoutDirection)
        .coerceAtLeast(SketchesDimens.LazyGridItemSpacing)
    val contentPaddingBottom = contentPaddings
        .calculateBottomPadding()
        .plus(
            if (overlayBottom) {
                SketchesDimens.LazyGridOverlayBottom
            } else {
                SketchesDimens.LazyGridItemSpacing
            },
        )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = SketchesDimens.LazyGridItemSize),
        modifier = modifier,
        state = state,
        contentPadding = PaddingValues(
            start = contentPaddingStart,
            top = contentPaddingTop,
            end = contentPaddingEnd,
            bottom = contentPaddingBottom,
        ),
        horizontalArrangement = Arrangement.spacedBy(space = SketchesDimens.LazyGridItemSpacing),
        verticalArrangement = Arrangement.spacedBy(space = SketchesDimens.LazyGridItemSpacing),
        content = content,
    )
}

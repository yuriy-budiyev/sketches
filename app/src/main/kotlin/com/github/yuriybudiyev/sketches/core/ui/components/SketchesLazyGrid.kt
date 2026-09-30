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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultAnimationSpec
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens

@Composable
fun rememberSketchesLazyGridSpec(
    itemSpacing: Dp = LocalDimens.current.lazyGridItemSpacing,
): SketchesLazyGridSpec {
    return remember { SketchesLazyGridSpec() }.also { spec ->
        spec.itemSpacing = itemSpacing.value
        spec.itemSpacingPx = with(LocalDensity.current) { itemSpacing.roundToPx() }
    }
}

@Stable
class SketchesLazyGridSpec {

    /**
     * Distance between two adjacent items in [Dp].
     *
     * Do not set manually, use [rememberSketchesLazyGridSpec].
     */
    var itemSpacing: Float by mutableFloatStateOf(0F)

    /**
     * Distance between two adjacent items in pixels.
     *
     * Do not set manually, use [rememberSketchesLazyGridSpec].
     */
    var itemSpacingPx: Int by mutableIntStateOf(0)
}

@Composable
fun SketchesLazyGrid(
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    spec: SketchesLazyGridSpec = rememberSketchesLazyGridSpec(),
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
    content: LazyGridScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val contentPaddings = WindowInsets.systemBars
        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
        .asPaddingValues()
    val dimens = LocalDimens.current
    val contentPaddingStart = contentPaddings
        .calculateStartPadding(layoutDirection)
        .coerceAtLeast(dimens.lazyGridItemSpacing)
    val contentPaddingTop = contentPaddings
        .calculateTopPadding()
        .plus(
            if (overlayTop) {
                dimens.lazyGridOverlayTop
            } else {
                dimens.lazyGridItemSpacing
            },
        )
    val contentPaddingEnd = contentPaddings
        .calculateEndPadding(layoutDirection)
        .coerceAtLeast(dimens.lazyGridItemSpacing)
    val contentPaddingBottom = contentPaddings
        .calculateBottomPadding()
        .plus(
            if (overlayBottom) {
                dimens.lazyGridOverlayBottom
            } else {
                dimens.lazyGridItemSpacing
            },
        )
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = dimens.lazyGridItemSize),
        modifier = modifier,
        state = state,
        contentPadding = PaddingValues(
            start = contentPaddingStart,
            top = contentPaddingTop,
            end = contentPaddingEnd,
            bottom = contentPaddingBottom,
        ),
        horizontalArrangement = Arrangement.spacedBy(space = spec.itemSpacing.dp),
        verticalArrangement = Arrangement.spacedBy(space = spec.itemSpacing.dp),
        content = content,
    )
}

/**
 * For [SketchesLazyGrid] with single content type and same sized items
 */
suspend fun LazyGridState.fastAnimateScrollToStart(spec: SketchesLazyGridSpec) {
    if (layoutInfo.totalItemsCount == 0) {
        return
    }
    if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) {
        return
    }
    val item = layoutInfo.visibleItemsInfo.firstOrNull() ?: return
    val itemSize = when (layoutInfo.orientation) {
        Orientation.Vertical -> item.size.height
        Orientation.Horizontal -> item.size.width
    }
    val scrollThreshold = when (layoutInfo.orientation) {
        Orientation.Vertical -> layoutInfo.viewportSize.height
        Orientation.Horizontal -> layoutInfo.viewportSize.width
    }
    val maxSpan = layoutInfo.maxSpan
    var scrollSpans = scrollThreshold / itemSize
    if (scrollThreshold % itemSize > 0) {
        scrollSpans++
    }
    var scrollIndex = scrollSpans * maxSpan
    var scrollAmount = scrollSpans * itemSize + scrollSpans * spec.itemSpacingPx
    if (firstVisibleItemIndex > scrollIndex) {
        scrollToItem(index = scrollIndex)
        animateScrollBy(
            value = -scrollAmount.toFloat(),
            animationSpec = defaultAnimationSpec(),
        )
        return
    }
    scrollIndex = firstVisibleItemIndex
    scrollSpans = scrollIndex / maxSpan
    if (scrollIndex % maxSpan > 0) {
        scrollSpans++
    }
    scrollAmount = scrollSpans * itemSize + scrollSpans * spec.itemSpacingPx + firstVisibleItemScrollOffset
    animateScrollBy(
        value = -scrollAmount.toFloat(),
        animationSpec = defaultAnimationSpec(),
    )
}

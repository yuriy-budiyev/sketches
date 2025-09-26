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

package com.github.yuriybudiyev.sketches.core.ui.utils

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.unit.IntSize

suspend inline fun LazyListState.scrollToItemCentered(
    index: Int,
    animate: Boolean = false,
    itemSize: (layoutInfo: LazyListLayoutInfo) -> Int = { layoutInfo ->
        layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
    },
) {
    val orientationAwareViewportSize = when (layoutInfo.orientation) {
        Orientation.Vertical -> layoutInfo.viewportSize.height
        Orientation.Horizontal -> layoutInfo.viewportSize.width
    }
    val offset = orientationAwareViewportSize
        .minus(layoutInfo.beforeContentPadding)
        .minus(layoutInfo.afterContentPadding)
        .minus(itemSize(layoutInfo))
        .div(2)
        .unaryMinus()
    if (animate) {
        animateScrollToItem(
            index = index,
            scrollOffset = offset,
        )
    } else {
        scrollToItem(
            index = index,
            scrollOffset = offset,
        )
    }
}

suspend inline fun LazyGridState.scrollToItemCentered(
    index: Int,
    animate: Boolean = false,
    itemSize: (layoutInfo: LazyGridLayoutInfo) -> IntSize = { layoutInfo ->
        layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: IntSize.Zero
    },
) {
    val orientationAwareViewportSize = when (layoutInfo.orientation) {
        Orientation.Vertical -> layoutInfo.viewportSize.height
        Orientation.Horizontal -> layoutInfo.viewportSize.width
    }
    val itemSize = itemSize(layoutInfo)
    val orientationAwareItemSize = when (layoutInfo.orientation) {
        Orientation.Vertical -> itemSize.height
        Orientation.Horizontal -> itemSize.width
    }
    val offset = orientationAwareViewportSize
        .minus(layoutInfo.beforeContentPadding)
        .minus(layoutInfo.afterContentPadding)
        .minus(orientationAwareItemSize)
        .div(2)
        .unaryMinus()
    if (animate) {
        animateScrollToItem(
            index = index,
            scrollOffset = offset,
        )
    } else {
        scrollToItem(
            index = index,
            scrollOffset = offset,
        )
    }
}

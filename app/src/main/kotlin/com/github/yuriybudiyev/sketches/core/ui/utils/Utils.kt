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
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.unit.IntSize

suspend fun LazyListState.scrollToItemCentered(
    index: Int,
    animate: Boolean = false,
    itemSize: suspend (layoutInfo: LazyListLayoutInfo) -> Int = { layoutInfo ->
        val visibleItemsInfo = layoutInfo.visibleItemsInfo
        visibleItemsInfo.find { it.index == index }?.size
            ?: visibleItemsInfo.firstOrNull()?.size
            ?: 0
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

suspend fun LazyGridState.scrollToItemClosestEdge(
    index: Int,
    animate: Boolean = false,
    onlyIfItemAtIndexIsNotVisible: Boolean = true,
    itemSize: suspend (layoutInfo: LazyGridLayoutInfo, itemInfo: LazyGridItemInfo?) -> IntSize =
        { layoutInfo, itemInfo ->
            itemInfo?.size ?: IntSize.Zero
        },
) {
    val firstItem = layoutInfo.visibleItemsInfo.firstOrNull()
    val itemAtIndex = layoutInfo.visibleItemsInfo.find { info -> info.index == index }
    val orientationAwareViewportSize = when (layoutInfo.orientation) {
        Orientation.Vertical -> layoutInfo.viewportSize.height
        Orientation.Horizontal -> layoutInfo.viewportSize.width
    }
    val viewportSizeWithAppliedPaddings = orientationAwareViewportSize
        .minus(layoutInfo.beforeContentPadding)
        .minus(layoutInfo.afterContentPadding)
    if (onlyIfItemAtIndexIsNotVisible && itemAtIndex != null) {
        val orientationAwareItemOffset = when (layoutInfo.orientation) {
            Orientation.Vertical -> itemAtIndex.offset.y
            Orientation.Horizontal -> itemAtIndex.offset.x
        }
        val orientationAwareItemSize = when (layoutInfo.orientation) {
            Orientation.Vertical -> itemAtIndex.size.height
            Orientation.Horizontal -> itemAtIndex.size.width
        }
        if (
            orientationAwareItemOffset >= 0
            && orientationAwareItemOffset + orientationAwareItemSize <= viewportSizeWithAppliedPaddings
        ) {
            return
        }
    }
    val itemSize = itemSize(
        layoutInfo,
        itemAtIndex ?: firstItem,
    )
    val orientationAwareItemSize = when (layoutInfo.orientation) {
        Orientation.Vertical -> itemSize.height
        Orientation.Horizontal -> itemSize.width
    }
    var offset = 0
    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull()
    if (firstItem != null && lastItem != null && firstItem !== lastItem) {
        if (index > firstItem.index + (lastItem.index - firstItem.index) / 2) {
            offset = viewportSizeWithAppliedPaddings
                .minus(orientationAwareItemSize)
                .unaryMinus()
        }
    }
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

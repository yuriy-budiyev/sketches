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

package com.github.yuriybudiyev.sketches.core.ui.utils

import androidx.annotation.FloatRange
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastFirstOrNull

suspend fun LazyGridState.scrollToItem(
    index: Int,
    itemType: Any?,
    animate: Boolean = false,
    snapToClosestEdge: Boolean = false,
    onlyIfItemAtIndexIsNotVisible: Boolean = false,
) {
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    val firstItemOfType = visibleItemsInfo.firstOrNull { info -> info.contentType == itemType }
    val itemAtIndex = visibleItemsInfo.firstOrNull { info -> info.index == index }
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
    val itemSize = itemAtIndex?.size ?: firstItemOfType?.size ?: IntSize.Zero
    val orientationAwareItemSize = when (layoutInfo.orientation) {
        Orientation.Vertical -> itemSize.height
        Orientation.Horizontal -> itemSize.width
    }
    var offset = 0
    if (snapToClosestEdge) {
        val lastItem = visibleItemsInfo.lastOrNull()
        if (firstItemOfType != null && lastItem != null && firstItemOfType !== lastItem) {
            if (index > firstItemOfType.index + (lastItem.index - firstItemOfType.index) / 2) {
                offset = viewportSizeWithAppliedPaddings
                    .minus(orientationAwareItemSize)
                    .unaryMinus()
            }
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

fun LazyGridState.findFirstVisibleItem(
    @FloatRange(
        from = 0.0,
        fromInclusive = true,
        to = 1.0,
        toInclusive = true,
    )
    factor: Float = 1F,
): LazyGridItemInfo? =
    with(layoutInfo) {
        when (orientation) {
            Orientation.Vertical -> {
                visibleItemsInfo.fastFirstOrNull { item ->
                    item.offset.y >= -(item.size.height * (1F - factor))
                }
            }
            Orientation.Horizontal -> {
                visibleItemsInfo.fastFirstOrNull { item ->
                    item.offset.x >= -(item.size.width * (1F - factor))
                }
            }
        }
    }

fun LazyGridState.findFirstVisibleItemIndex(
    @FloatRange(
        from = 0.0,
        fromInclusive = true,
        to = 1.0,
        toInclusive = true,
    )
    factor: Float = 1F,
): Int =
    findFirstVisibleItem(factor)?.index ?: -1

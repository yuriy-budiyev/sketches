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

import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastFirstOrNull

suspend fun LazyGridState.scrollToItem(
    index: Int,
    contentType: Any?,
    snapToClosestEdge: Boolean,
    onlyIfItemAtIndexIsNotVisible: Boolean,
) {
    val itemAtIndex = layoutInfo.visibleItemsInfo.fastFirstOrNull { info -> info.index == index }
    val viewportSizeWithPaddings =
        layoutInfo.viewportSize.mainAxis(layoutInfo.orientation) -
            layoutInfo.beforeContentPadding -
            layoutInfo.afterContentPadding
    if (onlyIfItemAtIndexIsNotVisible && itemAtIndex != null) {
        val itemOffset = itemAtIndex.offset.mainAxis(layoutInfo.orientation)
        val itemSize = itemAtIndex.size.mainAxis(layoutInfo.orientation)
        if (itemOffset >= 0 && itemOffset + itemSize <= viewportSizeWithPaddings) {
            return
        }
    }
    var offset = 0
    if (snapToClosestEdge) {
        val firstItem = layoutInfo.visibleItemsInfo.firstOrNull()
        val lastItem = layoutInfo.visibleItemsInfo.lastOrNull()
        if (firstItem != null && lastItem != null && firstItem !== lastItem) {
            if (index > firstItem.index + (lastItem.index - firstItem.index) / 2) {
                var itemSize = itemAtIndex?.size
                if (itemSize == null) {
                    itemSize = layoutInfo.visibleItemsInfo
                        .fastFirstOrNull { info -> info.contentType == contentType }?.size
                }
                if (itemSize == null) {
                    itemSize = IntSize.Zero
                }
                offset = -(viewportSizeWithPaddings - itemSize.mainAxis(layoutInfo.orientation))
            }
        }
    }
    scrollToItem(
        index = index,
        scrollOffset = offset,
    )
}

fun LazyGridState.getItemInfoAtOffset(offset: Offset): LazyGridItemInfo? {
    val startOffsetX: Int
    val startOffsetY: Int
    when (layoutInfo.orientation) {
        Orientation.Vertical -> {
            startOffsetX = 0
            startOffsetY = layoutInfo.viewportStartOffset
        }
        Orientation.Horizontal -> {
            startOffsetX = layoutInfo.viewportStartOffset
            startOffsetY = 0
        }
    }
    val offsetX = offset.x.roundToInt() + startOffsetX
    val offsetY = offset.y.roundToInt() + startOffsetY
    val viewportSize = layoutInfo.viewportSize
    if (offsetX < 0 || offsetY < 0 || offsetX > viewportSize.width || offsetY > viewportSize.height) {
        return null
    }
    return layoutInfo.visibleItemsInfo.fastFirstOrNull { info ->
        val itemStartOffset = info.offset
        val itemSize = info.size
        val itemStartOffsetX = itemStartOffset.x
        val itemStartOffsetY = itemStartOffset.y
        val itemEndOffsetX = itemStartOffsetX + itemSize.width
        val itemEndOffsetY = itemStartOffsetY + itemSize.height
        offsetX >= itemStartOffsetX && offsetX <= itemEndOffsetX &&
            offsetY >= itemStartOffsetY && offsetY <= itemEndOffsetY
    }
}

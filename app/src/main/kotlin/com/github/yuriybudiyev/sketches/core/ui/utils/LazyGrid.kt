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

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastFirstOrNull

suspend fun LazyGridState.scrollToItem(
    index: Int,
    itemType: Any?,
    snapToClosestEdge: Boolean = false,
    onlyIfItemAtIndexIsNotVisible: Boolean = false,
) {
    val layoutInfo = layoutInfo
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    val firstItemOfType = visibleItemsInfo.fastFirstOrNull { info -> info.contentType == itemType }
    val itemAtIndex = visibleItemsInfo.fastFirstOrNull { info -> info.index == index }
    val orientation = layoutInfo.orientation
    val viewportSize = layoutInfo.viewportSize.mainAxis(orientation)
    val viewportSizeWithAppliedPaddings =
        viewportSize - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding
    if (onlyIfItemAtIndexIsNotVisible && itemAtIndex != null) {
        val itemOffset = itemAtIndex.offset.mainAxis(orientation)
        val itemSize = itemAtIndex.size.mainAxis(orientation)
        if (itemOffset >= 0 && itemOffset + itemSize <= viewportSizeWithAppliedPaddings) {
            return
        }
    }
    val itemSize = itemAtIndex?.size ?: firstItemOfType?.size ?: IntSize.Zero
    val orientationAwareItemSize = itemSize.mainAxis(orientation)
    var offset = 0
    if (snapToClosestEdge) {
        val lastItem = visibleItemsInfo.lastOrNull()
        if (firstItemOfType != null && lastItem != null && firstItemOfType !== lastItem) {
            if (index > firstItemOfType.index + (lastItem.index - firstItemOfType.index) / 2) {
                offset = -(viewportSizeWithAppliedPaddings - orientationAwareItemSize)
            }
        }
    }
    scrollToItem(
        index = index,
        scrollOffset = offset,
    )
}

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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.util.fastFirstOrNull
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultAnimationSpec

/**
 * For lists with same constant item size
 */
suspend fun LazyListState.scrollToItemCentered(index: Int) {
    if (layoutInfo.totalItemsCount == 0) {
        return
    }
    val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return
    val viewportCenter = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
    scrollToItem(
        index = index,
        scrollOffset = -(viewportCenter - itemSize / 2),
    )
}

/**
 * For lists with same constant item size
 */
suspend fun LazyListState.fastAnimateScrollToItemCentered(index: Int) {
    if (layoutInfo.totalItemsCount == 0) {
        return
    }
    val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return
    val viewportCenter = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2
    var targetItem = layoutInfo.visibleItemsInfo.fastFirstOrNull { item -> item.index == index }
    if (targetItem != null) {
        animateScrollBy(
            value = (targetItem.offset - viewportCenter + itemSize / 2).toFloat(),
            animationSpec = defaultAnimationSpec(),
        )
    } else {
        val viewportSize = when (layoutInfo.orientation) {
            Orientation.Vertical -> layoutInfo.viewportSize.height
            Orientation.Horizontal -> layoutInfo.viewportSize.width
        }
        scrollToItem(
            index = index,
            scrollOffset = if (index > firstVisibleItemIndex) -(viewportSize - itemSize) else 0,
        )
        targetItem = layoutInfo.visibleItemsInfo.fastFirstOrNull { item -> item.index == index }
        if (targetItem != null) {
            animateScrollBy(
                value = (targetItem.offset - viewportCenter + itemSize / 2).toFloat(),
                animationSpec = defaultAnimationSpec(),
            )
        } else {
            scrollToItem(
                index = index,
                scrollOffset = -(viewportCenter - itemSize / 2),
            )
        }
    }
}

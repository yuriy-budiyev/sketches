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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs

@Composable
fun rememberLastScrolledScrollConnection(state: LazyGridState): LastScrolledScrollConnection {
    val state by rememberUpdatedState(state)
    val orientation by remember {
        derivedStateOf(structuralEqualityPolicy()) {
            state.layoutInfo.orientation
        }
    }
    return rememberLastScrolledScrollConnection(orientation)
}

@Composable
fun rememberLastScrolledScrollConnection(orientation: Orientation): LastScrolledScrollConnection =
    remember { LastScrolledScrollConnectionImpl() }.apply {
        update(
            orientation = orientation,
            direction = LocalLayoutDirection.current,
            threshold = LocalViewConfiguration.current.touchSlop,
        )
    }

@Stable
interface LastScrolledScrollConnection: NestedScrollConnection {

    val neverScrolled: Boolean

    val lastScrolledForward: Boolean

    val lastScrolledBackward: Boolean

    fun reset()
}

@Stable
private class LastScrolledScrollConnectionImpl: LastScrolledScrollConnection {

    fun update(
        orientation: Orientation,
        direction: LayoutDirection,
        threshold: Float,
    ) {
        var changed = false
        if (this.orientation != orientation) {
            this.orientation = orientation
            changed = true
        }
        if (this.direction != direction) {
            this.direction = direction
            changed = true
        }
        if (this.threshold != threshold) {
            this.threshold = threshold
            changed = true
        }
        if (changed) {
            reset()
        }
    }

    override val neverScrolled: Boolean by derivedStateOf(structuralEqualityPolicy()) {
        !lastScrolledForward && !lastScrolledBackward
    }

    override var lastScrolledForward: Boolean by mutableStateOf(false)
        private set

    override var lastScrolledBackward: Boolean by mutableStateOf(false)
        private set

    override fun reset() {
        lastScrolledForward = false
        lastScrolledBackward = false
        accumulated = 0F
    }

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val orientation = orientation
        val delta = when (orientation) {
            Orientation.Vertical -> available.y
            Orientation.Horizontal -> available.x
        }
        accumulated += delta
        if (abs(accumulated) >= threshold) {
            val forward =
                if (orientation == Orientation.Horizontal && direction == LayoutDirection.Rtl) {
                    accumulated > 0F
                } else {
                    accumulated < 0F
                }
            if (forward) {
                lastScrolledBackward = false
                lastScrolledForward = true
            } else {
                lastScrolledForward = false
                lastScrolledBackward = true
            }
            accumulated = 0F
        }
        return Offset.Zero
    }

    private var orientation: Orientation = Orientation.Vertical
    private var direction: LayoutDirection = LayoutDirection.Ltr
    private var threshold: Float = 0F
    private var accumulated: Float = 0F
}

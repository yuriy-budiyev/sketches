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

package com.github.yuriybudiyev.sketches.core.platform.insets

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.Insets

@Stable
class MutableAndroidWindowInsets: WindowInsets {

    override fun getLeft(
        density: Density,
        layoutDirection: LayoutDirection,
    ): Int =
        left.intValue

    override fun getTop(density: Density): Int =
        top.intValue

    override fun getRight(
        density: Density,
        layoutDirection: LayoutDirection,
    ): Int =
        right.intValue

    override fun getBottom(density: Density): Int =
        bottom.intValue

    fun update(insets: Insets) {
        left.intValue = insets.left
        top.intValue = insets.top
        right.intValue = insets.right
        bottom.intValue = insets.bottom
    }

    private val left: MutableIntState = mutableIntStateOf(0)
    private val top: MutableIntState = mutableIntStateOf(0)
    private val right: MutableIntState = mutableIntStateOf(0)
    private val bottom: MutableIntState = mutableIntStateOf(0)
}

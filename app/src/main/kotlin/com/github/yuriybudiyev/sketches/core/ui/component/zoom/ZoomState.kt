/*
 * MIT License
 *
 * Copyright (c) 2023 Yuriy Budiyev
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

package com.github.yuriybudiyev.sketches.core.ui.component.zoom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun rememberZoomState(): ZoomState =
    remember { ZoomStateImpl() }

@Stable
interface ZoomState {

    val scaleX: Float

    val scaleY: Float

    suspend fun scale(
        x: Float,
        y: Float
    )

    suspend fun animateScale(
        x: Float,
        y: Float
    )

    val translationX: Float

    val translationY: Float

    suspend fun translation(
        x: Float,
        y: Float
    )

    suspend fun animateTranslation(
        x: Float,
        y: Float
    )
}

@Stable
private class ZoomStateImpl(): ZoomState {

    override var scaleX: Float by mutableFloatStateOf(1f)
        private set

    override var scaleY: Float by mutableFloatStateOf(1f)
        private set

    override suspend fun scale(
        x: Float,
        y: Float
    ) {
        this.scaleX = x
        this.scaleY = y
    }

    override suspend fun animateScale(
        x: Float,
        y: Float
    ) {
        TODO("Not yet implemented")
    }

    override var translationX: Float by mutableFloatStateOf(0f)
        private set

    override var translationY: Float by mutableFloatStateOf(0f)
        private set

    override suspend fun translation(
        x: Float,
        y: Float
    ) {
        this.translationX = x
        this.translationY = y
    }

    override suspend fun animateTranslation(
        x: Float,
        y: Float
    ) {
        TODO("Not yet implemented")
    }
}

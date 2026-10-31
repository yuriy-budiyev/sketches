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

package com.github.yuriybudiyev.sketches.core.ui.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

const val DefaultSpringStiffness: Float = 1000F
const val DefaultSpringDampingRatio: Float = 1F

@Stable
@Suppress("UNCHECKED_CAST")
fun <T> defaultAnimationSpec(): SpringSpec<T> =
    DefaultAnimationSpec as SpringSpec<T>

@Stable
fun defaultMotionScheme(): MotionScheme =
    DefaultMotionScheme

context(scope: LazyItemScope)
fun Modifier.defaultAnimateItem(): Modifier =
    with(scope) {
        animateItem(
            fadeInSpec = defaultAnimationSpec(),
            fadeOutSpec = defaultAnimationSpec(),
            placementSpec = defaultAnimationSpec(),
        )
    }

context(scope: LazyGridItemScope)
fun Modifier.defaultAnimateItem(): Modifier =
    with(scope) {
        animateItem(
            fadeInSpec = defaultAnimationSpec(),
            fadeOutSpec = defaultAnimationSpec(),
            placementSpec = defaultAnimationSpec(),
        )
    }

@Composable
inline fun DefaultAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val visible by rememberUpdatedState(visible)
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1F else 0F,
        animationSpec = defaultAnimationSpec(),
    )
    val contentInComposition by remember { computedStateOf { visible || contentAlpha > 0F } }
    if (contentInComposition) {
        Box(
            modifier = Modifier
                .graphicsLayer { alpha = contentAlpha }
                .then(modifier),
            propagateMinConstraints = true,
            content = { content() },
        )
    }
}

private val DefaultAnimationSpec: SpringSpec<Any> =
    SpringSpec(
        dampingRatio = DefaultSpringDampingRatio,
        stiffness = DefaultSpringStiffness,
        visibilityThreshold = null,
    )

@Suppress("UNCHECKED_CAST")
private data object DefaultMotionScheme: MotionScheme {

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
        DefaultAnimationSpec as FiniteAnimationSpec<T>

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
        DefaultAnimationSpec as FiniteAnimationSpec<T>

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
        DefaultAnimationSpec as FiniteAnimationSpec<T>

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        DefaultAnimationSpec as FiniteAnimationSpec<T>

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
        DefaultAnimationSpec as FiniteAnimationSpec<T>

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
        DefaultAnimationSpec as FiniteAnimationSpec<T>
}

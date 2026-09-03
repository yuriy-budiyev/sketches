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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Stable
val DefaultScaleAnimationSpec: FiniteAnimationSpec<Float> =
    SpringSpec(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.002F,
    )

@Stable
val DefaultAlphaAnimationSpec: FiniteAnimationSpec<Float> =
    SpringSpec(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.003921569F,
    )

@Stable
val DefaultColorAnimationSpec: FiniteAnimationSpec<Color> =
    SpringSpec(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = Color(0x01010101),
    )

@Stable
val DefaultDpAnimationSpec: FiniteAnimationSpec<Dp> =
    SpringSpec(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = Dp(0.1F),
    )

@Stable
val DefaultPxAnimationSpec: FiniteAnimationSpec<Float> =
    SpringSpec(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = 0.1F,
    )

@Stable
val DefaultEnterTransition: EnterTransition =
    fadeIn(
        animationSpec = DefaultAlphaAnimationSpec,
        initialAlpha = 0F,
    )

@Stable
val DefaultExitTransition: ExitTransition =
    fadeOut(
        animationSpec = DefaultAlphaAnimationSpec,
        targetAlpha = 0F,
    )

@Composable
@NonRestartableComposable
inline fun DefaultAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    crossinline content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = DefaultEnterTransition,
        exit = DefaultExitTransition,
        label = "DefaultAnimatedVisibility",
        content = { content() },
    )
}

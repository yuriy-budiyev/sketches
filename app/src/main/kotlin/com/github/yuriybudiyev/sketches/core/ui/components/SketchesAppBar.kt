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

package com.github.yuriybudiyev.sketches.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yuriybudiyev.sketches.core.ui.animation.DefaultAlphaAnimationSpec
import com.github.yuriybudiyev.sketches.core.ui.colors.withLowTransparency
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens

@Composable
inline fun BoxScope.SketchesTopAppBar(
    text: String? = null,
    visible: Boolean = true,
    actions: @Composable () -> Unit = {},
) {
    val layoutDirection = LocalLayoutDirection.current
    val paddings = WindowInsets.systemBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
        .asPaddingValues()
    val contentPaddingTop = paddings.calculateTopPadding()
    val appBarAlpha by animateFloatAsState(
        targetValue = if (visible) 1F else 0F,
        animationSpec = DefaultAlphaAnimationSpec,
    )
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth(),
    ) {
        if (contentPaddingTop > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentPaddingTop)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colorScheme.background,
                                colorScheme.background.withLowTransparency(),
                            ),
                        ),
                        shape = RectangleShape,
                    ),
            )
        }
        if (appBarAlpha > 0F) {
            SketchesAppBar(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = appBarAlpha
                    }
                    .fillMaxWidth()
                    .background(
                        color = colorScheme.background.withLowTransparency(),
                        shape = RectangleShape,
                    ),
                text = text,
                contentPaddingStart = paddings.calculateStartPadding(layoutDirection),
                contentPaddingEnd = paddings.calculateEndPadding(layoutDirection),
                contentPaddingBottom = paddings.calculateBottomPadding(),
                actions = actions,
            )
        }
    }
}

@Composable
inline fun SketchesAppBar(
    modifier: Modifier = Modifier,
    text: String? = null,
    contentPaddingStart: Dp = 0.dp,
    contentPaddingTop: Dp = 0.dp,
    contentPaddingEnd: Dp = 0.dp,
    contentPaddingBottom: Dp = 0.dp,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .padding(
                top = contentPaddingTop,
                bottom = contentPaddingBottom,
            )
            .height(LocalDimens.current.material3AppBarHeight)
            .padding(
                start = contentPaddingStart + 16.dp,
                end = contentPaddingEnd + 4.dp,
            ),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!text.isNullOrEmpty()) {
            Text(
                text = text,
                modifier = Modifier.weight(1F),
                fontSize = 22.sp,
                lineHeight = 28.sp,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
        }
        actions()
    }
}

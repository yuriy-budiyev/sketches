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

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SketchesActionButton(
    icon: Painter,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.onBackground,
    hintPosition: ActionButtonHintPosition = ActionButtonHintPosition.Start,
) {
    TooltipBox(
        state = rememberTooltipState(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            when (hintPosition) {
                ActionButtonHintPosition.Above -> TooltipAnchorPosition.Above
                ActionButtonHintPosition.Below -> TooltipAnchorPosition.Below
                ActionButtonHintPosition.Start -> TooltipAnchorPosition.Start
                ActionButtonHintPosition.End -> TooltipAnchorPosition.End
            },
        ),
        tooltip = {
            PlainTooltip(
                shape = RectangleShape,
            ) {
                Text(
                    text = hint,
                    fontSize = 16.sp,
                )
            }
        },
    ) {
    }

    var hintVisible by remember { mutableStateOf(false) }
    var anchorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        Modifier
            .onGloballyPositioned { coordinates ->
                anchorCoordinates = coordinates
            }
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = true,
                onClickLabel = hint,
                role = Role.Button,
                onClick = onClick,
            )
            .then(modifier),
        Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = hint,
            tint = iconColor,
        )
    }
}

enum class ActionButtonHintPosition {

    Above,
    Below,
    Start,
    End,
}

@Composable
private fun rememberHintPositionProvider(position: ActionButtonHintPosition): HintPositionProvider {
    val offset = with(LocalDensity.current) { 4.dp.roundToPx() }
    return remember(position, offset) { HintPositionProvider(position, offset) }
}

private class HintPositionProvider(
    private val position: ActionButtonHintPosition,
    private val offset: Int,
): PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset =
        when (position) {
            ActionButtonHintPosition.Above -> {
                calculatePositionAbove(anchorBounds, windowSize, popupContentSize)
            }
            ActionButtonHintPosition.Below -> {
                calculatePositionBelow(anchorBounds, windowSize, popupContentSize)
            }
            ActionButtonHintPosition.Start -> {
                when (layoutDirection) {
                    LayoutDirection.Ltr -> {
                        calculatePositionLeft(anchorBounds, popupContentSize)
                    }
                    LayoutDirection.Rtl -> {
                        calculatePositionRight(anchorBounds, windowSize, popupContentSize)
                    }
                }
            }
            ActionButtonHintPosition.End -> {
                when (layoutDirection) {
                    LayoutDirection.Ltr -> {
                        calculatePositionRight(anchorBounds, windowSize, popupContentSize)
                    }
                    LayoutDirection.Rtl -> {
                        calculatePositionLeft(anchorBounds, popupContentSize)
                    }
                }
            }
        }

    private fun calculatePositionAbove(
        anchorBounds: IntRect,
        windowSize: IntSize,
        popupContentSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        when {
            x < 0 -> {
                x = anchorBounds.left
            }
            x + popupContentSize.width > windowSize.width -> {
                x = anchorBounds.right - popupContentSize.width
            }
        }
        var y = anchorBounds.top - popupContentSize.height - offset
        if (y < 0) {
            y = anchorBounds.bottom + offset
        }
        return IntOffset(x, y)
    }

    private fun calculatePositionBelow(
        anchorBounds: IntRect,
        windowSize: IntSize,
        popupContentSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        when {
            x < 0 -> {
                x = anchorBounds.left
            }
            x + popupContentSize.width > windowSize.width -> {
                x = anchorBounds.right - popupContentSize.width
            }
        }
        var y = anchorBounds.bottom + offset
        if (y + popupContentSize.height > windowSize.height) {
            y = anchorBounds.top - popupContentSize.height - offset
        }
        return IntOffset(x, y)
    }

    private fun calculatePositionLeft(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.left - (popupContentSize.width + offset)
        if (x < 0) {
            x = anchorBounds.right + offset
        }
        val y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }

    private fun calculatePositionRight(
        anchorBounds: IntRect,
        windowSize: IntSize,
        popupContentSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.right + offset
        if (x + popupContentSize.width > windowSize.width) {
            x = anchorBounds.left - (popupContentSize.width + offset)
        }
        val y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }
}

@Composable
fun SketchesAppBarActionButton(
    @DrawableRes
    iconRes: Int,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size = 48.dp)
            .clip(shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

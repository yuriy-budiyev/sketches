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

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultAnimationSpec
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens
import com.github.yuriybudiyev.sketches.core.ui.theme.withLowTransparency
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SketchesActionButton(
    icon: Painter,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.onBackground,
    hintPosition: ActionButtonHintPosition = ActionButtonHintPosition.Start,
) {
    val onClick by rememberUpdatedState(onClick)
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = MaterialTheme.colorScheme
    val shapes = MaterialTheme.shapes
    val dimens = LocalDimens.current
    val hintPositionProvider = remember(hintPosition) { HintPositionProvider(hintPosition) }
    var hintVisible by remember { mutableStateOf(false) }
    val hintAlpha by animateFloatAsState(
        targetValue = if (hintVisible) 1F else 0F,
        animationSpec = defaultAnimationSpec(),
    )
    val hintInComposition by remember {
        derivedStateOf(structuralEqualityPolicy()) {
            hintVisible || hintAlpha > 0F
        }
    }
    var hideHintJob by remember { mutableStateOf<Job?>(null) }
    val view = LocalView.current
    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (hintInComposition) {
            Popup(
                popupPositionProvider = hintPositionProvider,
                onDismissRequest = {
                    hintVisible = false
                },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    securePolicy = SecureFlagPolicy.Inherit,
                    excludeFromSystemGesture = true,
                    clippingEnabled = true,
                ),
            ) {
                val popupView = LocalView.current
                SideEffect(popupView) {
                    var view: View? = popupView
                    while (view != null) {
                        view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        view = view.parent as? View
                    }
                }
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = hintAlpha
                        }
                        .padding(all = 8.dp)
                        .dropShadow(
                            shape = shapes.extraSmall,
                            shadow = Shadow(
                                radius = dimens.shadowBlurRadius,
                                color = colorScheme.scrim.withLowTransparency(),
                            ),
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = colorScheme.surfaceContainerHigh,
                                shape = shapes.extraSmall,
                            )
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp,
                            ),
                    ) {
                        Text(
                            text = hint,
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .semantics {
                    role = Role.Button
                    contentDescription = hint
                    onLongClick {
                        hideHintJob?.cancel()
                        coroutineScope.launch {
                            hintVisible = true
                        }
                        true
                    }
                    onClick {
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        coroutineScope.launch {
                            onClick()
                        }
                        true
                    }
                }
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(),
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val press = PressInteraction.Press(offset)
                            coroutineScope.launch {
                                interactionSource.emit(press)
                            }
                            val released = tryAwaitRelease()
                            coroutineScope.launch {
                                interactionSource.emit(
                                    if (released) {
                                        PressInteraction.Release(press)
                                    } else {
                                        PressInteraction.Cancel(press)
                                    },
                                )
                            }
                            hideHintJob?.cancel()
                            hideHintJob = coroutineScope.launch {
                                delay(timeMillis = 1500L)
                                hintVisible = false
                            }
                        },
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            hideHintJob?.cancel()
                            coroutineScope.launch {
                                hintVisible = true
                            }
                        },
                        onTap = {
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            coroutineScope.launch {
                                onClick()
                            }
                        },
                    )
                }
                .then(modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = hint,
                tint = iconColor,
            )
        }
    }
}

enum class ActionButtonHintPosition {

    Above,
    Below,
    Start,
    End,
}

private class HintPositionProvider(
    private val position: ActionButtonHintPosition,
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
                x = 0
            }
            x + popupContentSize.width > windowSize.width -> {
                x = windowSize.width - popupContentSize.width
            }
        }
        var y = anchorBounds.top - popupContentSize.height
        if (y < 0) {
            y = anchorBounds.bottom
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
                x = 0
            }
            x + popupContentSize.width > windowSize.width -> {
                x = windowSize.width - popupContentSize.width
            }
        }
        var y = anchorBounds.bottom
        if (y + popupContentSize.height > windowSize.height) {
            y = anchorBounds.top - popupContentSize.height
        }
        return IntOffset(x, y)
    }

    private fun calculatePositionLeft(
        anchorBounds: IntRect,
        popupContentSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.left - popupContentSize.width
        if (x < 0) {
            x = 0
        }
        val y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }

    private fun calculatePositionRight(
        anchorBounds: IntRect,
        windowSize: IntSize,
        popupContentSize: IntSize,
    ): IntOffset {
        var x = anchorBounds.right
        if (x + popupContentSize.width > windowSize.width) {
            x = windowSize.width - popupContentSize.width
        }
        val y = (anchorBounds.top + anchorBounds.bottom - popupContentSize.height) / 2
        return IntOffset(x, y)
    }
}

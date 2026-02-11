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

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SketchesSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme
        .colorScheme
        .onBackground,
    trackColor: Color = MaterialTheme
        .colorScheme
        .onBackground
        .copy(alpha = SketchesColors.UiAlphaLowTransparency),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val trackHeight = 4.dp
    val thumbSize = DpSize(
        width = 20.dp,
        height = 20.dp,
    )
    val colors = SliderDefaults.colors(
        thumbColor = thumbColor,
        activeTrackColor = trackColor,
        activeTickColor = trackColor,
        inactiveTrackColor = trackColor,
        inactiveTickColor = trackColor,
        disabledThumbColor = thumbColor,
        disabledActiveTrackColor = trackColor,
        disabledActiveTickColor = trackColor,
        disabledInactiveTrackColor = trackColor,
        disabledInactiveTickColor = trackColor,
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = Modifier
            .requiredSizeIn(
                minWidth = thumbSize.width,
                minHeight = trackHeight,
            )
            .then(modifier),
        interactionSource = interactionSource,
        colors = colors,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                modifier = Modifier
                    .size(thumbSize)
                    .shadow(
                        1.dp,
                        CircleShape,
                        clip = false,
                    )
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple(
                            bounded = false,
                            radius = 20.dp,
                        ),
                    ),
                colors = colors,
            )
        },
        track = {
            SliderDefaults.Track(
                sliderState = it,
                modifier = Modifier.height(trackHeight),
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 0.dp,
                drawStopIndicator = null,
                colors = colors,
            )
        },
    )
}

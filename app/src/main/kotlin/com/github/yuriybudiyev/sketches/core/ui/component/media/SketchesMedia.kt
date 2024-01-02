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

package com.github.yuriybudiyev.sketches.core.ui.component.media

import kotlin.math.roundToLong
import android.view.TextureView
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons

@Composable
fun SketchesMediaPlayer(
    state: SketchesMediaState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    controlsBackgroundColor: Color = backgroundColor.copy(alpha = 0.5f),
    controlsColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Box(modifier = modifier) {
        SketchesMediaDisplay(
            state = state,
            modifier = Modifier.matchParentSize(),
            backgroundColor = backgroundColor
        )
        SketchesMediaController(
            state = state,
            modifier = Modifier
                .height(height = 56.dp)
                .padding(horizontal = 4.dp)
                .background(
                    color = controlsBackgroundColor,
                    shape = RectangleShape
                )
                .align(alignment = Alignment.BottomCenter),
            color = controlsColor
        )
    }
}

@Composable
fun SketchesMediaDisplay(
    state: SketchesMediaState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = backgroundColor,
                    shape = RectangleShape
                )
        )
        val displayAspectRatio = state.displayAspectRatio
        val context by rememberUpdatedState(LocalContext.current)
        AndroidView(modifier = Modifier
            .aspectRatio(
                ratio = displayAspectRatio,
                matchHeightConstraintsFirst = displayAspectRatio < 1f
            )
            .align(Alignment.Center),
            factory = { TextureView(context) },
            update = { view -> state.setVideoView(view) })
        if (!state.isVideoVisible) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = backgroundColor,
                        shape = RectangleShape
                    )
            )
        }
    }
}

@Composable
fun SketchesMediaController(
    state: SketchesMediaState,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val coroutineScope = rememberCoroutineScope()
        Box(modifier = Modifier
            .size(size = 48.dp)
            .clip(shape = CircleShape)
            .clickable {
                coroutineScope.launch {
                    if (state.isPlaying) {
                        state.pause()
                    } else {
                        state.play()
                    }
                }
            },
            contentAlignment = Alignment.Center,
            content = {
                Icon(
                    imageVector = if (state.isPlaying) {
                        SketchesIcons.Pause
                    } else {
                        SketchesIcons.Play
                    },
                    contentDescription = null,
                    modifier = Modifier.size(size = 24.dp)
                )
            })
        val position = state.position
        val duration = state.duration
        Slider(
            value = if (position != SketchesMediaState.TIME_UNKNOWN && duration != SketchesMediaState.TIME_UNKNOWN) {
                (position.toDouble() / duration.toDouble()).toFloat()
            } else {
                0f
            },
            modifier = Modifier.weight(1f),
            onValueChange = { value ->
                coroutineScope.launch {
                    if (duration != SketchesMediaState.TIME_UNKNOWN) {
                        state.seek((duration.toDouble() * value.toDouble()).roundToLong())
                    }
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                activeTickColor = color,
                inactiveTrackColor = color,
                inactiveTickColor = color,
                disabledThumbColor = color,
                disabledActiveTrackColor = color,
                disabledActiveTickColor = color,
                disabledInactiveTrackColor = color,
                disabledInactiveTickColor = color
            )
        )
        Box(modifier = Modifier
            .size(size = 48.dp)
            .clip(shape = CircleShape)
            .clickable {
                coroutineScope.launch {
                    if (state.isVolumeEnabled) {
                        state.disableVolume()
                    } else {
                        state.enableVolume()
                    }
                }
            },
            contentAlignment = Alignment.Center,
            content = {
                Icon(
                    imageVector = if (state.isVolumeEnabled) {
                        SketchesIcons.VolumeEnabled
                    } else {
                        SketchesIcons.VolumeDisabled
                    },
                    contentDescription = null,
                    modifier = Modifier.size(size = 24.dp)
                )
            })
    }
}

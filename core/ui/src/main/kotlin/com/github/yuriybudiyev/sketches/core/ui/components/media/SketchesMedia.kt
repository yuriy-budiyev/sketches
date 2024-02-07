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

package com.github.yuriybudiyev.sketches.core.ui.components.media

import kotlin.math.roundToLong
import android.view.TextureView
import kotlinx.coroutines.CoroutineScope
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
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.yuriybudiyev.sketches.core.ui.R
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons

@Composable
fun SketchesMediaPlayer(
    state: SketchesMediaState,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    controlsBackgroundColor: Color = backgroundColor.copy(alpha = 0.75f),
    controlsColor: Color = MaterialTheme.colorScheme.onBackground,
    enableImagePlaceholder: Boolean = true,
    enableErrorIndicator: Boolean = true,
) {
    Box(modifier = modifier) {
        SketchesMediaDisplay(
            state = state,
            modifier = Modifier.matchParentSize(),
            backgroundColor = backgroundColor,
            enableImagePlaceholder = enableImagePlaceholder,
            enableErrorIndicator = enableErrorIndicator
        )
        SketchesMediaController(
            state = state,
            modifier = Modifier
                .background(
                    color = controlsBackgroundColor,
                    shape = RectangleShape
                )
                .height(height = 64.dp)
                .padding(horizontal = 4.dp)
                .align(alignment = Alignment.BottomCenter),
            coroutineScope = coroutineScope,
            color = controlsColor
        )
    }
}

@Composable
fun SketchesMediaDisplay(
    state: SketchesMediaState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    enableImagePlaceholder: Boolean = true,
    enableErrorIndicator: Boolean = true,
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
        val videoVisible = state.isVideoVisible
        key(
            displayAspectRatio,
            videoVisible
        ) {
            AndroidView(
                modifier = Modifier
                    .aspectRatio(
                        ratio = displayAspectRatio,
                        matchHeightConstraintsFirst = displayAspectRatio < 1f
                    )
                    .align(Alignment.Center),
                factory = { context -> TextureView(context) },
                update = { view -> state.setVideoView(view) },
                onReset = { view -> state.clearVideoView(view) },
                onRelease = { view -> state.clearVideoView(view) },
            )
            if (!videoVisible) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            color = backgroundColor,
                            shape = RectangleShape
                        )
                ) {
                    if (state.isPlaybackError) {
                        if (enableErrorIndicator) {
                            Icon(
                                imageVector = SketchesIcons.ImageError,
                                contentDescription = stringResource(id = R.string.image_error),
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        if (enableImagePlaceholder) {
                            Icon(
                                imageVector = SketchesIcons.ImageLoading,
                                contentDescription = stringResource(id = R.string.image),
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SketchesMediaController(
    state: SketchesMediaState,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                    contentDescription = stringResource(
                        id = if (state.isPlaying) {
                            R.string.pause
                        } else {
                            R.string.play
                        }
                    ),
                    modifier = Modifier.size(size = 24.dp)
                )
            })
        val position = state.position
        val duration = state.duration
        Slider(
            value = if (position != SketchesMediaState.UnknownTime && duration != SketchesMediaState.UnknownTime) {
                (position.toDouble() / duration.toDouble()).toFloat()
            } else {
                0f
            },
            modifier = Modifier.weight(weight = 1f),
            onValueChange = { value ->
                if (duration != SketchesMediaState.UnknownTime) {
                    val newPosition = (duration.toDouble() * value.toDouble()).roundToLong()
                    coroutineScope.launch {
                        state.seek(newPosition)
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
                    contentDescription = stringResource(
                        id = if (state.isVolumeEnabled) {
                            R.string.disable_volume
                        } else {
                            R.string.enable_volume
                        }
                    ),
                    modifier = Modifier.size(size = 24.dp)
                )
            })
    }
}

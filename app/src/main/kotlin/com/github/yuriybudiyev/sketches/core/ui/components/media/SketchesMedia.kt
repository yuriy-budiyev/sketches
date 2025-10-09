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

import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesSlider
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

@Composable
fun SketchesMediaPlayer(
    state: SketchesMediaState,
    controllerVisible: Boolean,
    modifier: Modifier = Modifier,
    controllerStartPadding: Dp = 0.dp,
    controllerEndPadding: Dp = 0.dp,
    controllerBottomPadding: Dp = 0.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    controlsBackgroundColor: Color = backgroundColor
        .copy(alpha = SketchesColors.UiAlphaLowTransparency),
    controlsColor: Color = MaterialTheme.colorScheme.onBackground,
    enableImagePlaceholder: Boolean = true,
    enableErrorIndicator: Boolean = true,
) {
    val controllerVisibleUpdated by rememberUpdatedState(controllerVisible)
    val controllerStartPaddingUpdated by rememberUpdatedState(controllerStartPadding)
    val controllerEndPaddingUpdated by rememberUpdatedState(controllerEndPadding)
    val controllerBottomPaddingUpdated by rememberUpdatedState(controllerBottomPadding)
    Box(modifier = modifier) {
        SketchesMediaDisplay(
            state = state,
            modifier = Modifier.matchParentSize(),
            backgroundColor = backgroundColor,
            indicatorColor = controlsColor,
            enableImagePlaceholder = enableImagePlaceholder,
            enableErrorIndicator = enableErrorIndicator
        )
        AnimatedVisibility(
            visible = controllerVisibleUpdated,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(alignment = Alignment.BottomStart)
        ) {
            SketchesMediaController(
                state = state,
                modifier = Modifier
                    .padding(bottom = controllerBottomPaddingUpdated)
                    .height(height = 64.dp)
                    .background(
                        color = controlsBackgroundColor,
                        shape = RectangleShape
                    )
                    .padding(
                        start = controllerStartPaddingUpdated + 4.dp,
                        end = controllerEndPaddingUpdated + 4.dp
                    )
                    .clickable(
                        interactionSource = null,
                        indication = null,
                    ) {
                        // Do nothing
                    },
                color = controlsColor,
            )
        }
    }
}

@Composable
fun SketchesMediaDisplay(
    state: SketchesMediaState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    indicatorColor: Color = MaterialTheme.colorScheme.onBackground,
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
        AndroidView(
            modifier = Modifier
                .aspectRatio(
                    ratio = displayAspectRatio,
                    matchHeightConstraintsFirst = displayAspectRatio < 1.0f
                )
                .align(Alignment.Center),
            factory = { context -> TextureView(context) },
            update = { view -> state.setVideoView(view) },
            onReset = { view -> state.clearVideoView(view) },
            onRelease = { view -> state.clearVideoView(view) },
        )
        if (!state.isVideoVisible) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = backgroundColor,
                        shape = RectangleShape
                    )
            )
            if (state.isPlaybackError) {
                if (enableErrorIndicator) {
                    Icon(
                        imageVector = SketchesIcons.ImageError,
                        contentDescription = stringResource(R.string.image_error),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        tint = indicatorColor
                    )
                }
            } else {
                if (enableImagePlaceholder) {
                    Icon(
                        imageVector = SketchesIcons.ImageLoading,
                        contentDescription = stringResource(R.string.image),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        tint = indicatorColor
                    )
                }
            }
        }
    }
}

@Composable
fun SketchesMediaController(
    state: SketchesMediaState,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(size = 48.dp)
                .clip(shape = CircleShape)
                .clickable {
                    state.coroutineScope.launch {
                        if (state.isPlaying) {
                            state.pause()
                        } else {
                            state.play()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
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
        }
        val position = state.position
        val duration = state.duration
        var seeking by remember { mutableStateOf(false) }
        var playingOnSeek by remember { mutableStateOf(false) }
        SketchesSlider(
            value = if (
                position != SketchesMediaState.UnknownTime
                && duration != SketchesMediaState.UnknownTime
            ) {
                (position.toDouble() / duration.toDouble()).toFloat()
            } else {
                0.0f
            },
            onValueChange = { value ->
                if (!seeking) {
                    seeking = true
                    playingOnSeek = state.isPlaying
                    if (playingOnSeek) {
                        state.coroutineScope.launch {
                            state.pause()
                        }
                    }
                }
                if (duration != SketchesMediaState.UnknownTime) {
                    val newPosition = (duration.toDouble() * value.toDouble()).roundToLong()
                    state.coroutineScope.launch {
                        state.seek(newPosition)
                    }
                }
            },
            onValueChangeFinished = {
                if (playingOnSeek) {
                    playingOnSeek = false
                    state.coroutineScope.launch {
                        state.play()
                    }
                }
                seeking = false
            },
            modifier = Modifier.weight(weight = 1.0f),
            thumbColor = color,
            trackColor = color.copy(alpha = SketchesColors.UiAlphaLowTransparency)
        )
        Box(
            modifier = Modifier
                .size(size = 48.dp)
                .clip(shape = CircleShape)
                .clickable {
                    state.coroutineScope.launch {
                        if (state.isVolumeEnabled) {
                            state.disableVolume()
                        } else {
                            state.enableVolume()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
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
        }
    }
}

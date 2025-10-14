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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesSlider
import com.github.yuriybudiyev.sketches.core.ui.components.detectTransformGestures
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.math.roundToLong

@Composable
fun SketchesMediaPlayer(
    state: SketchesMediaState,
    onDisplayTap: () -> Unit,
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
    val onDisplayTapUpdated by rememberUpdatedState(onDisplayTap)
    val controllerVisibleUpdated by rememberUpdatedState(controllerVisible)
    val controllerStartPaddingUpdated by rememberUpdatedState(controllerStartPadding)
    val controllerEndPaddingUpdated by rememberUpdatedState(controllerEndPadding)
    val controllerBottomPaddingUpdated by rememberUpdatedState(controllerBottomPadding)
    Box(modifier = modifier) {
        SketchesMediaDisplay(
            state = state,
            modifier = Modifier.matchParentSize(),
            onTap = onDisplayTapUpdated,
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
    onTap: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    indicatorColor: Color = MaterialTheme.colorScheme.onBackground,
    enableImagePlaceholder: Boolean = true,
    enableErrorIndicator: Boolean = true,
    maxRelativeZoom: Float = 10f,
    doubleTapZoomFraction: Float = 0.2f,
) {
    require(maxRelativeZoom >= 1f) {
        "Maximum relative zoom can't be lower than 1.0"
    }
    require(doubleTapZoomFraction >= 0f && doubleTapZoomFraction <= 1f) {
        "Double tap zoom fraction should be in 0.0 to 1.0 range"
    }
    val coroutineScope = rememberCoroutineScope()
    val onTapUpdated by rememberUpdatedState(onTap)
    val doubleTapZoomFractionUpdated by rememberUpdatedState(doubleTapZoomFraction)
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var contentSize by remember { mutableStateOf(Size.Zero) }
    var minScale by remember { mutableFloatStateOf(0f) }
    var maxScale by remember { mutableFloatStateOf(0f) }
    val scale = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        snapshotFlow { containerSize to contentSize }.collect { (containerSize, contentSize) ->
            if (containerSize != Size.Zero && contentSize != Size.Zero) {
                val fitScaleWidth = containerSize.width / contentSize.width
                val fitScaleHeight = containerSize.height / contentSize.height
                val fitScale = min(
                    fitScaleWidth,
                    fitScaleHeight,
                )
                minScale = fitScale
                maxScale = fitScale * maxRelativeZoom
                scale.updateBounds(
                    minScale,
                    maxScale,
                )
                if (scale.value == 0f) {
                    scale.snapTo(fitScale)
                    offsetX.snapTo(0f)
                    offsetX.snapTo(0f)
                } else {
                    val maxOffsetX =
                        (containerSize.width - (contentSize.width * scale.value)).absoluteValue / 2f
                    val maxOffsetY =
                        (containerSize.height - (contentSize.height * scale.value)).absoluteValue / 2f
                    val newOffsetX = offsetX.value.coerceIn(
                        -maxOffsetX,
                        +maxOffsetX,
                    )
                    val newOffsetY = offsetY.value.coerceIn(
                        -maxOffsetY,
                        +maxOffsetY
                    )
                    offsetX.snapTo(newOffsetX)
                    offsetY.snapTo(newOffsetY)
                }
            }
        }
    }
    LaunchedEffect(maxRelativeZoom) {
        maxScale = minScale * maxRelativeZoom
        scale.updateBounds(
            minScale,
            maxScale,
        )
    }
    Box(
        modifier = modifier
            .onSizeChanged { size ->
                containerSize = size.toSize()
            }
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { centroid, pan, zoom ->
                        coroutineScope.launch {
                            val newScale = (scale.value * zoom).coerceIn(
                                minimumValue = minScale,
                                maximumValue = maxScale,
                            )
                            val scaleFactor = newScale / scale.value
                            val scaledContentWidth = contentSize.width * newScale
                            val scaledContentHeight = contentSize.height * newScale
                            val unusedContainerWidth = containerSize.width - scaledContentWidth
                            val unusedContainerHeight = containerSize.height - scaledContentHeight
                            val relativeCentroid = containerSize.center - centroid
                            val newOffsetX = if (unusedContainerWidth < 0f) {
                                ((offsetX.value + relativeCentroid.x) * scaleFactor - relativeCentroid.x + pan.x)
                                    .coerceIn(
                                        -unusedContainerWidth.absoluteValue / 2f,
                                        +unusedContainerWidth.absoluteValue / 2f,
                                    )
                            } else {
                                0f
                            }
                            val newOffsetY = if (unusedContainerHeight < 0f) {
                                ((offsetY.value + relativeCentroid.y) * scaleFactor - relativeCentroid.y + pan.y)
                                    .coerceIn(
                                        -unusedContainerHeight.absoluteValue / 2f,
                                        +unusedContainerHeight.absoluteValue / 2f,
                                    )
                            } else {
                                0f
                            }
                            scale.snapTo(newScale)
                            offsetX.snapTo(newOffsetX)
                            offsetY.snapTo(newOffsetY)
                        }
                    },
                    onAfterGesture = { change ->
                        if (scale.value > minScale) {
                            change.consume()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            val newScale: Float
                            val newOffsetX: Float
                            val newOffsetY: Float
                            if (scale.value == minScale) {
                                newScale =
                                    minScale + ((maxScale - minScale) * doubleTapZoomFractionUpdated)
                                val scaleFactor = newScale / scale.value
                                val scaledContentWidth = contentSize.width * newScale
                                val scaledContentHeight = contentSize.height * newScale
                                val unusedContainerWidth = containerSize.width - scaledContentWidth
                                val unusedContainerHeight =
                                    containerSize.height - scaledContentHeight
                                val relativeTapOffset = containerSize.center - tapOffset
                                newOffsetX = if (unusedContainerWidth < 0f) {
                                    ((offsetX.value + relativeTapOffset.x) * scaleFactor - relativeTapOffset.x)
                                        .coerceIn(
                                            -unusedContainerWidth.absoluteValue / 2f,
                                            +unusedContainerWidth.absoluteValue / 2f,
                                        )
                                } else {
                                    0f
                                }
                                newOffsetY = if (unusedContainerHeight < 0f) {
                                    ((offsetY.value + relativeTapOffset.y) * scaleFactor - relativeTapOffset.y)
                                        .coerceIn(
                                            -unusedContainerHeight.absoluteValue / 2f,
                                            +unusedContainerHeight.absoluteValue / 2f,
                                        )
                                } else {
                                    0f
                                }
                            } else {
                                newScale = minScale
                                newOffsetX = 0f
                                newOffsetY = 0f
                            }
                            val scaleJob = launch {
                                scale.animateTo(
                                    newScale,
                                    tween(),
                                )
                            }
                            val offsetXJob = launch {
                                offsetX.animateTo(
                                    newOffsetX,
                                    tween(),
                                )
                            }
                            val offsetYJob = launch {
                                offsetY.animateTo(
                                    newOffsetY,
                                    tween(),
                                )
                            }
                            scaleJob.join()
                            offsetXJob.join()
                            offsetYJob.join()
                        }
                    },
                    onTap = {
                        onTapUpdated?.invoke()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = backgroundColor,
                    shape = RectangleShape
                ),
        )
        val displayAspectRatio = state.displayAspectRatio
        AndroidView(
            modifier = Modifier
                .align(Alignment.Center)
                /*.wrapContentSize(
                    align = Alignment.Center,
                    unbounded = true,
                )*/
                .aspectRatio(
                    ratio = displayAspectRatio,
                    matchHeightConstraintsFirst = displayAspectRatio < 1f
                )
                .onSizeChanged { size ->
                    contentSize = size.toSize()
                }
                .graphicsLayer {
                    translationX = offsetX.value
                    translationY = offsetY.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
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
                0f
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
            modifier = Modifier.weight(weight = 1f),
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

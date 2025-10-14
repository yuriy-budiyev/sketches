/*
 * MIT License
 *
 * Copyright (c) 2025 Yuriy Budiyev
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

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min

@Composable
fun SketchesZoomableBox(
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    @FloatRange(
        from = 1.0,
        fromInclusive = true,
    )
    maxRelativeZoom: Float = 10f,
    @FloatRange(
        from = 0.0,
        fromInclusive = true,
        to = 1.0,
        toInclusive = true,
    )
    doubleTapZoomFraction: Float = 0.2f,
    content: @Composable ZoomableBoxScope.() -> Unit,
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
    var zoomed by remember { mutableStateOf(false) }
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
                if (scale.value == 0f || !zoomed) {
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
                zoomed = scale.value > fitScale
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
                            zoomed = newScale > minScale
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
                            zoomed = newScale > minScale
                        }
                    },
                    onTap = {
                        onTapUpdated?.invoke()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val scope = remember { ZoomableBoxScopeImpl(this) }
        LaunchedEffect(Unit) {
            snapshotFlow { scope.contentSize }.collect { size ->
                contentSize = size
            }
        }
        scope.offsetX = offsetX.value
        scope.offsetY = offsetY.value
        scope.scale = scale.value
        scope.content()
    }
}

/**
 * Slightly changed version of [androidx.compose.foundation.gestures.detectTransformGestures]
 */
private suspend fun PointerInputScope.detectTransformGestures(
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onAfterGesture: (change: PointerInputChange) -> Unit,
) {
    awaitEachGesture {
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.fastAny { change -> change.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()
                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()
                    if (zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }
                if (pastTouchSlop) {
                    if (zoomChange != 1f || panChange != Offset.Zero) {
                        val centroid = event.calculateCentroid(useCurrent = true)
                        onGesture(
                            centroid,
                            panChange,
                            zoomChange,
                        )
                    }
                    event.changes.fastForEach { change ->
                        if (change.positionChanged()) {
                            onAfterGesture(change)
                        }
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { change -> change.pressed })
    }
}

@LayoutScopeMarker
@Stable
interface ZoomableBoxScope: BoxScope {

    @Stable
    fun Modifier.zoomable(): Modifier
}

@Stable
private class ZoomableBoxScopeImpl(private val boxScope: BoxScope): ZoomableBoxScope {

    var contentSize: Size by mutableStateOf(Size.Zero)

    var offsetX: Float by mutableFloatStateOf(0f)

    var offsetY: Float by mutableFloatStateOf(0f)

    var scale: Float by mutableFloatStateOf(0f)

    @Stable
    override fun Modifier.zoomable(): Modifier =
        this
            .onSizeChanged { size ->
                contentSize = size.toSize()
            }
            .graphicsLayer {
                translationX = offsetX
                translationY = offsetY
                scaleX = scale
                scaleY = scale
            }

    @Stable
    override fun Modifier.align(alignment: Alignment): Modifier =
        with(boxScope) { align(alignment) }

    @Stable
    override fun Modifier.matchParentSize(): Modifier =
        with(boxScope) { matchParentSize() }
}

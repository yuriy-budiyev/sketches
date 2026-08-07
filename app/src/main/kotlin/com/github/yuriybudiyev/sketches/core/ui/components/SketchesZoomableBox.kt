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

import android.os.Parcelable
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.min

@Composable
fun rememberZoomState(): ZoomState =
    rememberSaveable(saver = ZoomStateImplSaver()) { ZoomStateImpl() }

@Composable
fun SketchesZoomableBox(
    modifier: Modifier = Modifier,
    zoomState: ZoomState = rememberZoomState(),
    onTap: (() -> Unit)? = null,
    @FloatRange(
        from = 1.0,
        fromInclusive = true,
    )
    maxRelativeZoom: Float = 10F,
    @FloatRange(
        from = 1.0,
        fromInclusive = true,
    )
    doubleTapRelativeZoom: Float = 2.5F,
    content: @Composable SketchesZoomableBoxScope.() -> Unit,
) {
    require(zoomState is ZoomStateImpl) {
        "zoomState should be obtained only by call to rememberZoomState"
    }
    require(maxRelativeZoom >= 1F) {
        "maxRelativeZoom can't be lower than 1.0"
    }
    require(doubleTapRelativeZoom in 1F..maxRelativeZoom) {
        "doubleTapRelativeZoom should be in 1.0 to maxRelativeZoom range"
    }
    val coroutineScope = rememberCoroutineScope()
    val onTapUpdated by rememberUpdatedState(onTap)
    val doubleTapRelativeZoomUpdated by rememberUpdatedState(doubleTapRelativeZoom)
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var contentSize by remember { mutableStateOf(Size.Zero) }
    var minScale by remember { mutableFloatStateOf(0F) }
    var maxScale by remember { mutableFloatStateOf(0F) }
    val currentScale = remember { Animatable(0F) }
    val currentOffsetX = remember { Animatable(0F) }
    val currentOffsetY = remember { Animatable(0F) }
    var currentIsZoomed by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(zoomState) {
        snapshotFlow {
            SizeSpec(
                container = containerSize,
                content = contentSize,
            )
        }.distinctUntilChanged().collect { (containerSize, contentSize) ->
            if (containerSize != Size.Zero && contentSize != Size.Zero) {
                val fitScaleWidth = containerSize.width / contentSize.width
                val fitScaleHeight = containerSize.height / contentSize.height
                val fitScale = min(
                    fitScaleWidth,
                    fitScaleHeight,
                )
                minScale = fitScale
                maxScale = fitScale * maxRelativeZoom
                currentScale.updateBounds(
                    minScale,
                    maxScale,
                )
                if (currentScale.value == 0F || !currentIsZoomed) {
                    currentScale.snapTo(fitScale)
                    currentOffsetX.snapTo(0F)
                    currentOffsetX.snapTo(0F)
                } else {
                    val maxOffsetX =
                        (containerSize.width - (contentSize.width * currentScale.value)).absoluteValue / 2F
                    val maxOffsetY =
                        (containerSize.height - (contentSize.height * currentScale.value)).absoluteValue / 2F
                    val newOffsetX = currentOffsetX.value.coerceIn(
                        -maxOffsetX,
                        +maxOffsetX,
                    )
                    val newOffsetY = currentOffsetY.value.coerceIn(
                        -maxOffsetY,
                        +maxOffsetY,
                    )
                    currentOffsetX.snapTo(newOffsetX)
                    currentOffsetY.snapTo(newOffsetY)
                }
                if (!isInitialized) {
                    currentScale.snapTo(zoomState.scale)
                    currentOffsetX.snapTo(zoomState.offsetX)
                    currentOffsetY.snapTo(zoomState.offsetY)
                }
                currentIsZoomed = currentScale.value > fitScale
                isInitialized = true
            }
        }
    }
    LaunchedEffect(maxRelativeZoom) {
        maxScale = minScale * maxRelativeZoom
        currentScale.updateBounds(
            minScale,
            maxScale,
        )
    }
    suspend fun CoroutineScope.toggleZoom(offset: Offset = Offset.Zero) {
        val newScale: Float
        val newOffsetX: Float
        val newOffsetY: Float
        if (currentScale.value == minScale) {
            newScale = minScale * doubleTapRelativeZoomUpdated
            val scaleFactor = newScale / currentScale.value
            val scaledContentWidth = contentSize.width * newScale
            val scaledContentHeight = contentSize.height * newScale
            val unusedContainerWidth = containerSize.width - scaledContentWidth
            val unusedContainerHeight =
                containerSize.height - scaledContentHeight
            val relativeTapOffset = containerSize.center - offset
            newOffsetX = if (unusedContainerWidth < 0F) {
                ((currentOffsetX.value + relativeTapOffset.x) * scaleFactor - relativeTapOffset.x)
                    .coerceIn(
                        -unusedContainerWidth.absoluteValue / 2F,
                        +unusedContainerWidth.absoluteValue / 2F,
                    )
            } else {
                0F
            }
            newOffsetY = if (unusedContainerHeight < 0F) {
                ((currentOffsetY.value + relativeTapOffset.y) * scaleFactor - relativeTapOffset.y)
                    .coerceIn(
                        -unusedContainerHeight.absoluteValue / 2F,
                        +unusedContainerHeight.absoluteValue / 2F,
                    )
            } else {
                0F
            }
        } else {
            newScale = minScale
            newOffsetX = 0F
            newOffsetY = 0F
        }
        val scaleJob = launch {
            currentScale.animateTo(
                newScale,
                tween(),
            )
        }
        val offsetXJob = launch {
            currentOffsetX.animateTo(
                newOffsetX,
                tween(),
            )
        }
        val offsetYJob = launch {
            currentOffsetY.animateTo(
                newOffsetY,
                tween(),
            )
        }
        scaleJob.join()
        offsetXJob.join()
        offsetYJob.join()
        currentIsZoomed = newScale > minScale
    }
    LaunchedEffect(zoomState) {
        snapshotFlow { currentIsZoomed }
            .distinctUntilChanged().collect { isZoomed ->
                zoomState.isZoomed = isZoomed
            }
    }
    LaunchedEffect(zoomState) {
        snapshotFlow { zoomState.isZoomed }
            .distinctUntilChanged().collect { isZoomed ->
                if (isZoomed != currentIsZoomed) {
                    toggleZoom()
                }
            }
    }
    LaunchedEffect(zoomState) {
        snapshotFlow {
            ZoomSpec(
                scale = currentScale.value,
                offsetX = currentOffsetX.value,
                offsetY = currentOffsetY.value,
            )
        }.distinctUntilChanged().collect { (scale, offsetX, offsetY) ->
            if (isInitialized) {
                zoomState.scale = scale
                zoomState.offsetX = offsetX
                zoomState.offsetY = offsetY
            }
        }
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
                            val newScale = (currentScale.value * zoom).coerceIn(
                                minimumValue = minScale,
                                maximumValue = maxScale,
                            )
                            val scaleFactor = newScale / currentScale.value
                            val scaledContentWidth = contentSize.width * newScale
                            val scaledContentHeight = contentSize.height * newScale
                            val unusedContainerWidth = containerSize.width - scaledContentWidth
                            val unusedContainerHeight = containerSize.height - scaledContentHeight
                            val relativeCentroid = containerSize.center - centroid
                            val newOffsetX = if (unusedContainerWidth < 0F) {
                                ((currentOffsetX.value + relativeCentroid.x) * scaleFactor - relativeCentroid.x + pan.x)
                                    .coerceIn(
                                        -unusedContainerWidth.absoluteValue / 2F,
                                        +unusedContainerWidth.absoluteValue / 2F,
                                    )
                            } else {
                                0F
                            }
                            val newOffsetY = if (unusedContainerHeight < 0F) {
                                ((currentOffsetY.value + relativeCentroid.y) * scaleFactor - relativeCentroid.y + pan.y)
                                    .coerceIn(
                                        -unusedContainerHeight.absoluteValue / 2F,
                                        +unusedContainerHeight.absoluteValue / 2F,
                                    )
                            } else {
                                0F
                            }
                            currentScale.snapTo(newScale)
                            currentOffsetX.snapTo(newOffsetX)
                            currentOffsetY.snapTo(newOffsetY)
                            currentIsZoomed = newScale > minScale
                        }
                    },
                    onAfterGesture = { change ->
                        if (currentScale.value > minScale) {
                            change.consume()
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            toggleZoom(tapOffset)
                        }
                    },
                    onTap = {
                        onTapUpdated?.invoke()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val scope = remember { SketchesZoomableBoxScopeImpl(this) }
        LaunchedEffect(Unit) {
            snapshotFlow { scope.contentSize }
                .distinctUntilChanged().collect { size ->
                    contentSize = size
                }
        }
        scope.scale = currentScale.value
        scope.offsetX = currentOffsetX.value
        scope.offsetY = currentOffsetY.value
        scope.content()
    }
}

@Stable
@LayoutScopeMarker
sealed interface SketchesZoomableBoxScope: BoxScope {

    /**
     * Connects the element with [SketchesZoomableBox] to enable zoom.
     * Only one element in the scope can be zoomable.
     */
    @Stable
    fun Modifier.zoomable(): Modifier
}

@Stable
sealed interface ZoomState {

    var isZoomed: Boolean
}

@Immutable
private data class ZoomSpec(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
)

@Immutable
private data class SizeSpec(
    val container: Size,
    val content: Size,
)

@Stable
private class SketchesZoomableBoxScopeImpl(
    private val boxScope: BoxScope,
): SketchesZoomableBoxScope {

    var scale: Float by mutableFloatStateOf(0F)

    var offsetX: Float by mutableFloatStateOf(0F)

    var offsetY: Float by mutableFloatStateOf(0F)

    var contentSize: Size by mutableStateOf(Size.Zero)

    @Stable
    override fun Modifier.zoomable(): Modifier =
        this
            .align(Alignment.Center)
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

private class ZoomStateImpl: ZoomState {

    override var isZoomed: Boolean by mutableStateOf(false)

    var scale: Float = 0F
    var offsetX: Float = 0F
    var offsetY: Float = 0F
}

private class ZoomStateImplSaver: Saver<ZoomStateImpl, ZoomStateImplConfig> {

    override fun SaverScope.save(value: ZoomStateImpl): ZoomStateImplConfig =
        ZoomStateImplConfig(
            scale = value.scale,
            offsetX = value.offsetX,
            offsetY = value.offsetY,
        )

    override fun restore(value: ZoomStateImplConfig): ZoomStateImpl =
        ZoomStateImpl().apply {
            scale = value.scale
            offsetX = value.offsetX
            offsetY = value.offsetY
        }
}

@Parcelize
private data class ZoomStateImplConfig(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
): Parcelable

/**
 * Slightly changed version of [androidx.compose.foundation.gestures.detectTransformGestures]
 */
private suspend fun PointerInputScope.detectTransformGestures(
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    onAfterGesture: (change: PointerInputChange) -> Unit,
) {
    awaitEachGesture {
        var zoom = 1F
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
                    if (zoomChange != 1F || panChange != Offset.Zero) {
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

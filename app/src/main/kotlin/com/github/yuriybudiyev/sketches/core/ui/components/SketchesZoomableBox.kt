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
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEach
import com.github.yuriybudiyev.sketches.core.platform.log.logDebug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlin.math.abs

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
    maxZoom: Float = 10F,
    @FloatRange(
        from = 1.0,
        fromInclusive = true,
    )
    doubleTapZoom: Float = 2.5F,
    content: @Composable SketchesZoomableBoxScope.() -> Unit,
) {
    require(zoomState is ZoomStateImpl) {
        "zoomState should be obtained only by call to rememberZoomState"
    }
    require(maxZoom >= 1F) {
        "maxZoom can't be lower than 1.0"
    }
    require(doubleTapZoom in 1F..maxZoom) {
        "doubleTapZoom should be in 1.0 to maxZoom range"
    }
    val coroutineScope = rememberCoroutineScope()
    val onTapUpdated by rememberUpdatedState(onTap)
    val maxZoomUpdated by rememberUpdatedState(maxZoom)
    val doubleTapZoomUpdated by rememberUpdatedState(doubleTapZoom)
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var contentSize by remember { mutableStateOf(Size.Zero) }
    val currentScale = remember(zoomState) { Animatable(zoomState.scale) }
    val currentOffsetX = remember(zoomState) { Animatable(zoomState.offsetX) }
    val currentOffsetY = remember(zoomState) { Animatable(zoomState.offsetY) }
    LaunchedEffect(zoomState) {
        snapshotFlow {
            SizeSpec(
                container = containerSize,
                content = contentSize,
            )
        }.collect { (containerSize, contentSize) ->
            if (containerSize != Size.Zero && contentSize != Size.Zero) {
                logDebug(
                    "init " + ZoomSpec(
                        scale = currentScale.value,
                        offsetX = currentOffsetX.value,
                        offsetY = currentOffsetY.value,
                    ),
                )
                /*currentScale.updateBounds(
                    1F,
                    maxZoomUpdated,
                )
                var newScale: Float
                var newOffsetX: Float
                var newOffsetY: Float
                if (isInitialized) {
                    newScale = currentScale.value
                    newOffsetX = currentOffsetX.value
                    newOffsetX = currentOffsetY.value
                } else {

                }*/

                /*if (isInitialized) {
                    if (currentIsZoomed) {
                        val maxOffsetX =
                            abs(containerSize.width - (contentSize.width * currentScale.value)) / 2F
                        val maxOffsetY =
                            abs(containerSize.height - (contentSize.height * currentScale.value)) / 2F
                        val newOffsetX = currentOffsetX.value.fastCoerceIn(
                            -maxOffsetX,
                            +maxOffsetX,
                        )
                        val newOffsetY = currentOffsetY.value.fastCoerceIn(
                            -maxOffsetY,
                            +maxOffsetY,
                        )
                        currentOffsetX.snapTo(newOffsetX)
                        currentOffsetY.snapTo(newOffsetY)
                    } else {
                        currentScale.snapTo(1F)
                        currentOffsetX.snapTo(0F)
                        currentOffsetX.snapTo(0F)
                    }
                } else {
                    var targetScale = zoomState.scale
                    var targetOffsetX = zoomState.offsetX
                    var targetOffsetY = zoomState.offsetY
                    currentScale.snapTo(targetScale)
                    currentOffsetX.snapTo(targetOffsetX)
                    currentOffsetY.snapTo(targetOffsetY)
                }
                currentIsZoomed = currentScale.value > 1F
                isInitialized = true*/
            }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { maxZoomUpdated }.collect { scale ->
            currentScale.updateBounds(
                1F,
                scale,
            )
        }
    }
    suspend fun CoroutineScope.toggleZoom(
        target: Offset = Offset.Zero,
        animate: Boolean = true,
    ) {
        val newScale: Float
        val newOffsetX: Float
        val newOffsetY: Float
        if (currentScale.value == 1F) {
            newScale = doubleTapZoomUpdated
            val scaledContentWidth = contentSize.width * newScale
            val scaledContentHeight = contentSize.height * newScale
            val unusedContainerWidth = containerSize.width - scaledContentWidth
            val unusedContainerHeight = containerSize.height - scaledContentHeight
            val relativeTarget = containerSize.center - target
            newOffsetX = if (unusedContainerWidth < 0F) {
                val maxOffset = unusedContainerWidth / 2F
                (((currentOffsetX.value + relativeTarget.x) * newScale) - relativeTarget.x)
                    .fastCoerceIn(
                        +maxOffset,
                        -maxOffset,
                    )
            } else {
                0F
            }
            newOffsetY = if (unusedContainerHeight < 0F) {
                val maxOffset = unusedContainerHeight / 2F
                (((currentOffsetY.value + relativeTarget.y) * newScale) - relativeTarget.y)
                    .fastCoerceIn(
                        +maxOffset,
                        -maxOffset,
                    )
            } else {
                0F
            }
        } else {
            newScale = 1F
            newOffsetX = 0F
            newOffsetY = 0F
        }
        if (animate) {
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
        } else {
            currentScale.snapTo(newScale)
            currentOffsetX.snapTo(newOffsetX)
            currentOffsetY.snapTo(newOffsetY)
        }
    }
    LaunchedEffect(zoomState) {
        snapshotFlow { currentScale.value > 1F }
            .collect { zoomed ->
                zoomState.isZoomed = zoomed
            }
    }
    LaunchedEffect(zoomState) {
        zoomState.toggleZoom.collect { toggleZoom ->
            toggleZoom(animate = toggleZoom.animate)
        }
    }
    LaunchedEffect(zoomState) {
        snapshotFlow {
            ZoomSpec(
                scale = currentScale.value,
                offsetX = currentOffsetX.value,
                offsetY = currentOffsetY.value,
            )
        }.collect { (scale, offsetX, offsetY) ->
            zoomState.scale = scale
            zoomState.offsetX = offsetX
            zoomState.offsetY = offsetY
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
                            val newScale = (currentScale.value * zoom)
                                .fastCoerceIn(
                                    1F,
                                    maxZoomUpdated,
                                )
                            val scaleFactor = newScale / currentScale.value
                            val scaledContentWidth = contentSize.width * newScale
                            val scaledContentHeight = contentSize.height * newScale
                            val unusedContainerWidth = containerSize.width - scaledContentWidth
                            val unusedContainerHeight = containerSize.height - scaledContentHeight
                            val relativeCentroid = contentSize.center - centroid
                            val newOffsetX = if (unusedContainerWidth < 0F) {
                                val maxOffset = unusedContainerWidth / 2F
                                (((currentOffsetX.value + relativeCentroid.x) * scaleFactor) - relativeCentroid.x + pan.x)
                                    .fastCoerceIn(
                                        +maxOffset,
                                        -maxOffset,
                                    )
                            } else {
                                0F
                            }
                            val newOffsetY = if (unusedContainerHeight < 0F) {
                                val maxOffset = unusedContainerHeight / 2F
                                (((currentOffsetY.value + relativeCentroid.y) * scaleFactor) - relativeCentroid.y + pan.y)
                                    .fastCoerceIn(
                                        +maxOffset,
                                        -maxOffset,
                                    )
                            } else {
                                0F
                            }
                            currentScale.snapTo(newScale)
                            currentOffsetX.snapTo(newOffsetX)
                            currentOffsetY.snapTo(newOffsetY)
                        }
                    },
                    onAfterGesture = { change ->
                        if (currentScale.value > 1F) {
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
                .collect { size ->
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

    val isZoomed: Boolean

    suspend fun toggleZoom(animate: Boolean = true)
}

private object Defaults {

    const val Scale = 1F
    const val Offset = 0F
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

    var scale: Float by mutableFloatStateOf(Defaults.Scale)

    var offsetX: Float by mutableFloatStateOf(Defaults.Offset)

    var offsetY: Float by mutableFloatStateOf(Defaults.Offset)

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

    override suspend fun toggleZoom(animate: Boolean) {
        toggleZoom.emit(ToggleZoom(animate))
    }

    val toggleZoom: Flow<ToggleZoom>
        field: MutableSharedFlow<ToggleZoom> = MutableSharedFlow()

    data class ToggleZoom(val animate: Boolean)

    var scale: Float = Defaults.Scale
    var offsetX: Float = Defaults.Offset
    var offsetY: Float = Defaults.Offset
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

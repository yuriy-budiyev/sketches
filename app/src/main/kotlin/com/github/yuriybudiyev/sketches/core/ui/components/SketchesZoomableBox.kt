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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
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
    LaunchedEffect(Unit) {
        snapshotFlow { maxZoomUpdated }.collect { scale ->
            currentScale.updateBounds(
                1F,
                scale,
            )
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { containerSize to contentSize }.collect { (containerSize, contentSize) ->
            if (containerSize != Size.Zero && contentSize != Size.Zero) {
                invalidateZoom(
                    target = Offset.Zero,
                    shift = Offset.Zero,
                    containerSize = containerSize,
                    contentSize = contentSize,
                    currentScale = currentScale.value,
                    currentOffsetX = currentOffsetX.value,
                    currentOffsetY = currentOffsetY.value,
                    newScale = currentScale.value,
                ) { newScale, newOffsetX, newOffsetY ->
                    currentScale.snapTo(newScale)
                    currentOffsetX.snapTo(newOffsetX)
                    currentOffsetY.snapTo(newOffsetY)
                }
            }
        }
    }
    suspend fun CoroutineScope.toggleZoom(
        offset: Offset = Offset.Zero,
        animate: Boolean = true,
    ) {
        val scale = currentScale.value
        val target: Offset
        val newScale: Float
        if (scale == 1F) {
            target = offset
            newScale = doubleTapZoomUpdated
        } else {
            target = Offset.Zero
            newScale = 1F
        }
        invalidateZoom(
            target = target,
            shift = Offset.Zero,
            containerSize = containerSize,
            contentSize = contentSize,
            currentScale = scale,
            currentOffsetX = currentOffsetX.value,
            currentOffsetY = currentOffsetY.value,
            newScale = newScale,
        ) { newScale, newOffsetX, newOffsetY ->
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
    }
    LaunchedEffect(zoomState) {
        launch {
            zoomState.toggleZoom.collect { toggleZoom ->
                toggleZoom(animate = toggleZoom.animate)
            }
        }
        launch {
            snapshotFlow { currentScale.value > 1F }.collect { zoomed ->
                zoomState.isZoomed = zoomed
            }
        }
        launch {
            snapshotFlow { currentScale.value }.collect { scale ->
                zoomState.scale = scale
            }
        }
        launch {
            snapshotFlow { currentOffsetX.value }.collect { offsetX ->
                zoomState.offsetX = offsetX
            }
        }
        launch {
            snapshotFlow { currentOffsetY.value }.collect { offsetY ->
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
                            val scale = currentScale.value
                            invalidateZoom(
                                target = centroid,
                                shift = pan,
                                containerSize = containerSize,
                                contentSize = contentSize,
                                currentScale = scale,
                                currentOffsetX = currentOffsetX.value,
                                currentOffsetY = currentOffsetY.value,
                                newScale = (scale * zoom).fastCoerceIn(
                                    minimumValue = 1F,
                                    maximumValue = maxZoomUpdated,
                                ),
                            ) { newScale, newOffsetX, newOffsetY ->
                                currentScale.snapTo(newScale)
                                currentOffsetX.snapTo(newOffsetX)
                                currentOffsetY.snapTo(newOffsetY)
                            }
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
                            toggleZoom(
                                offset = tapOffset,
                                animate = true,
                            )
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
            snapshotFlow { scope.contentSize }.collect { size ->
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

@OptIn(ExperimentalContracts::class)
private inline fun invalidateZoom(
    target: Offset,
    shift: Offset,
    containerSize: Size,
    contentSize: Size,
    currentScale: Float,
    currentOffsetX: Float,
    currentOffsetY: Float,
    newScale: Float,
    invalidate: (newScale: Float, newOffsetX: Float, newOffsetY: Float) -> Unit,
) {
    contract {
        callsInPlace(
            invalidate,
            InvocationKind.EXACTLY_ONCE,
        )
    }
    val scaleFactor = newScale / currentScale
    val relativeTarget = containerSize.center - target
    val maxOffsetX = (containerSize.width - (contentSize.width * newScale)) / 2F
    val newOffsetX = if (maxOffsetX < 0F) {
        (((currentOffsetX + relativeTarget.x) * scaleFactor) - relativeTarget.x + shift.x)
            .fastCoerceIn(
                minimumValue = +maxOffsetX,
                maximumValue = -maxOffsetX,
            )
    } else {
        0F
    }
    val maxOffsetY = (containerSize.height - (contentSize.height * newScale)) / 2F
    val newOffsetY = if (maxOffsetY < 0F) {
        (((currentOffsetY + relativeTarget.y) * scaleFactor) - relativeTarget.y + shift.y)
            .fastCoerceIn(
                minimumValue = +maxOffsetY,
                maximumValue = -maxOffsetY,
            )
    } else {
        0F
    }
    invalidate(
        newScale,
        newOffsetX,
        newOffsetY,
    )
}

private suspend inline fun PointerInputScope.detectTransformGestures(
    crossinline onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit,
    crossinline onAfterGesture: (change: PointerInputChange) -> Unit,
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

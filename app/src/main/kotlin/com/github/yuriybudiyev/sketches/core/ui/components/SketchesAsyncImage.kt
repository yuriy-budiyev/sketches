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

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

@Composable
fun SketchesAsyncImage(
    uri: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    filterQuality: FilterQuality = FilterQuality.Low,
    enableLoadingIndicator: Boolean = true,
    enableErrorIndicator: Boolean = true,
) {
    SubcomposeAsyncImage(
        model = uri,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        filterQuality = filterQuality,
        loading = if (enableLoadingIndicator) {
            {
                StateIcon(
                    icon = SketchesIcons.ImageLoading,
                    description = stringResource(R.string.image_loading),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            null
        },
        error = if (enableErrorIndicator) {
            {
                StateIcon(
                    icon = SketchesIcons.ImageError,
                    description = stringResource(R.string.image_error),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            null
        },
    )
}

@Composable
fun SketchesZoomableAsyncImage(
    uri: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var painterState by remember {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val painter = rememberAsyncImagePainter(
        model = uri,
        onState = { state ->
            painterState = state
        },
        contentScale = ContentScale.None,
        filterQuality = FilterQuality.High
    )
    val minZoom = 1f
    val maxZoom = 5f
    val coroutineScope = rememberCoroutineScope()
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var contentSize by remember { mutableStateOf(Size.Zero) }
    var minScale by remember { mutableFloatStateOf(minZoom) }
    var maxScale by remember { mutableFloatStateOf(maxZoom) }
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
                maxScale = max(
                    fitScale * maxZoom,
                    1f,
                )
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
    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
            .onSizeChanged { size ->
                containerSize = size.toSize()
            }
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { pan, zoom ->
                        coroutineScope.launch {
                            val newScale = (scale.value * zoom).coerceIn(
                                minimumValue = minScale,
                                maximumValue = maxScale,
                            )
                            val scaledContentWidth = contentSize.width * newScale
                            val scaledContentHeight = contentSize.height * newScale
                            val containerWidthDiff = containerSize.width - scaledContentWidth
                            val containerHeightDiff = containerSize.height - scaledContentHeight
                            var newOffsetX = offsetX.value + pan.x
                            var newOffsetY = offsetY.value + pan.y
                            if (containerWidthDiff >= 0f) {
                                newOffsetX = 0f
                            }
                            if (containerHeightDiff >= 0f) {
                                newOffsetY = 0f
                            }
                            scale.snapTo(newScale)
                            offsetX.snapTo(
                                newOffsetX.coerceIn(
                                    -containerWidthDiff.absoluteValue / 2f,
                                    +containerWidthDiff.absoluteValue / 2f,
                                )
                            )
                            offsetY.snapTo(
                                newOffsetY.coerceIn(
                                    -containerHeightDiff.absoluteValue / 2f,
                                    +containerHeightDiff.absoluteValue / 2f,
                                )
                            )
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
                awaitEachGesture {
                    val firstDown = awaitFirstDown()
                    val firstUpOrCancel = waitForUpOrCancellation()
                    if (firstUpOrCancel != null) {
                        val secondDown =
                            withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
                                val minUptime =
                                    firstUpOrCancel.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
                                var change: PointerInputChange
                                do {
                                    change = awaitFirstDown()
                                } while (change.uptimeMillis < minUptime)
                                change
                            }
                        if (secondDown != null) {
                            val secondUpOrCancel = waitForUpOrCancellation()
                            if (secondUpOrCancel != null) {
                                firstDown.consume()
                                secondDown.consume()
                                secondUpOrCancel.consume()
                                coroutineScope.launch {
                                    scale.snapTo(1f)
                                }
                                // onDoubleTap
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .wrapContentSize(
                    align = Alignment.Center,
                    unbounded = true,
                )
                .onSizeChanged { size ->
                    contentSize = size.toSize()
                }
                .graphicsLayer {
                    translationX = offsetX.value
                    translationY = offsetY.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .paint(
                    painter,
                    contentScale = ContentScale.None,
                    alignment = Alignment.Center,
                ),
        )
    }
}

@Composable
private fun StateIcon(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(SketchesDimens.AsyncImageStateIconSize),
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * Slightly changed version of [androidx.compose.foundation.gestures.detectTransformGestures]
 */
private suspend fun PointerInputScope.detectTransformGestures(
    onGesture: (pan: Offset, zoom: Float) -> Unit,
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
                        onGesture(
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

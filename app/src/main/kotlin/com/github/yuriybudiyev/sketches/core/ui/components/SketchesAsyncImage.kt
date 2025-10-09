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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.platform.log.log
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import kotlinx.coroutines.launch
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
    val minZoom = 1F
    val maxZoom = 5F
    val coroutineScope = rememberCoroutineScope()
    var containerSize by remember { mutableStateOf(Size.Zero) }
    var contentSize by remember { mutableStateOf(Size.Zero) }
    var minScale by remember { mutableFloatStateOf(minZoom) }
    var maxScale by remember { mutableFloatStateOf(maxZoom) }
    val scale = remember { Animatable(0F) }
    val offsetX = remember { Animatable(0F) }
    val offsetY = remember { Animatable(0F) }
    LaunchedEffect(Unit) {
        snapshotFlow { containerSize to contentSize }.collect { (outerSize, innerSize) ->
            if (outerSize != Size.Zero && innerSize != Size.Zero) {
                val fitScale = min(
                    outerSize.width / innerSize.width,
                    outerSize.height / innerSize.height,
                )
                log("outer: $outerSize inner: $innerSize")
                minScale = fitScale
                maxScale = max(
                    fitScale * maxZoom,
                    1F,
                )
                log("min: $minScale max: $maxScale")
                scale.updateBounds(
                    lowerBound = minScale,
                    upperBound = maxScale,
                )
                scale.snapTo(fitScale)
            }
        }
    }
    // From YT Lesson

    fun getMaxOffset(
        size: Float,
        scale: Float,
    ): Float {
        val scaledSize = size * scale
        return max(
            (scaledSize - size) / 2F,
            0F,
        )
    }

    fun clampOffset(
        size: Float,
        offset: Float,
        scale: Float,
    ): Float {
        val maxOffset = getMaxOffset(
            size = size,
            scale = scale
        )
        return offset.coerceIn(
            minimumValue = -maxOffset,
            maximumValue = maxOffset,
        )
    }

    // End YT Lesson
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
                detectTransformGestures { _, pan, zoom, _ ->
                    coroutineScope.launch {
                        val newScale = (scale.value * zoom).coerceIn(
                            minimumValue = minScale,
                            maximumValue = maxScale,
                        )
                        val newOffsetX = offsetX.value + pan.x
                        val newOffsetY = offsetY.value + pan.y
                        val scaledContentWidth = contentSize.width * newScale
                        val containerWidthDiff = containerSize.width - scaledContentWidth
                        val scaledContentHeight = contentSize.height * newScale
                        val containerHeightDiff = containerSize.height - scaledContentHeight
                        log("-----")
                        log("container: $containerSize")
                        log("content: $contentSize")
                        log("scaled width diff: $containerWidthDiff")
                        log("scaled height diff: $containerHeightDiff")
                        log("scale: ${scale.value} -> $newScale")
                        log("offsetX: ${offsetX.value} -> $newOffsetX")
                        log("offsetY: ${offsetY.value} -> $newOffsetY")
                        log("-----")

                        scale.snapTo(newScale)
                        offsetX.snapTo(
                            newOffsetX.coerceIn(
                                -containerWidthDiff.absoluteValue / 2F,
                                containerWidthDiff.absoluteValue / 2F
                            )
                        )
                        offsetY.snapTo(
                            newOffsetY.coerceIn(
                                -containerHeightDiff.absoluteValue / 2F,
                                containerHeightDiff.absoluteValue / 2F
                            )
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
        /*.pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { offset ->
                    coroutineScope.launch {

                    }
                }
            )
        }*/
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .wrapContentSize(
                    align = Alignment.Center,
                    unbounded = true,
                )
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
                )
                .border(
                    width = 4.dp,
                    color = Color.Red,
                )
                .onSizeChanged { size ->
                    contentSize = size.toSize()
                },
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

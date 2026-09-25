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

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import com.github.yuriybudiyev.sketches.core.text.capitalizeFirstChar
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultAnimateItem
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLazyGrid
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens
import com.github.yuriybudiyev.sketches.core.ui.theme.withHighTransparency
import com.github.yuriybudiyev.sketches.core.ui.theme.withLowTransparency
import com.github.yuriybudiyev.sketches.core.ui.theme.withMediumTransparency
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Immutable
sealed interface SketchesMediaGridKey: Parcelable {

    @Parcelize
    @Immutable
    data class Header(
        val year: Int,
        val month: Int,
    ): SketchesMediaGridKey

    @Parcelize
    @Immutable
    data class Media(
        val id: Long,
    ): SketchesMediaGridKey
}

@Immutable
sealed interface SketchesMediaGridContentType {

    @Immutable
    data object Header: SketchesMediaGridContentType

    @Immutable
    data object Media: SketchesMediaGridContentType
}

@Composable
fun SketchesMediaGrid(
    files: List<MediaFile>,
    selectedFiles: SnapshotStateSet<Long>,
    onItemClick: (index: Int, file: MediaFile) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
) {
    val files by rememberUpdatedState(files)
    val selectedFiles by rememberUpdatedState(selectedFiles)
    val onItemClick by rememberUpdatedState(onItemClick)
    SketchesLazyGrid(
        modifier = modifier,
        state = state,
        overlayTop = overlayTop,
        overlayBottom = overlayBottom,
    ) {
        items(
            count = files.size,
            key = { index -> SketchesMediaGridKey.Media(id = files[index].id) },
            contentType = { SketchesMediaGridContentType.Media },
        ) { index ->
            val file by rememberUpdatedState(files[index])
            val fileSelected by remember {
                derivedStateOf(structuralEqualityPolicy()) {
                    selectedFiles.contains(file.id)
                }
            }
            MediaItem(
                file = file,
                fileSelected = fileSelected,
                onLongClick = {
                    if (selectedFiles.isEmpty()) {
                        selectedFiles.add(file.id)
                    }
                },
                onClick = {
                    if (selectedFiles.isNotEmpty()) {
                        if (fileSelected) {
                            selectedFiles.remove(file.id)
                        } else {
                            selectedFiles.add(file.id)
                        }
                    } else {
                        onItemClick(index, file)
                    }
                },
                modifier = Modifier.defaultAnimateItem(),
            )
        }
    }
}

@Stable
class SketchesMediaGridScrollSpec {

    var headerItemSize: IntSize by mutableStateOf(IntSize.Zero)

    var mediaItemSize: IntSize by mutableStateOf(IntSize.Zero)

}

@Composable
fun SketchesGroupingMediaGrid(
    files: List<MediaFile>,
    selectedFiles: SnapshotStateSet<Long>,
    onItemClick: (index: Int, file: MediaFile) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    scrollSpec: SketchesMediaGridScrollSpec = remember { SketchesMediaGridScrollSpec() },
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
) {
    val files by rememberUpdatedState(files)
    val selectedFiles by rememberUpdatedState(selectedFiles)
    val onItemClick by rememberUpdatedState(onItemClick)
    val nowDate = remember { LocalDate.now() }
    val dateFormatterMonth = remember {
        DateTimeFormatterBuilder()
            .appendText(
                ChronoField.MONTH_OF_YEAR,
                TextStyle.FULL_STANDALONE,
            )
            .toFormatter(Locale.getDefault())
    }
    val dateFormatterMonthYear = remember {
        DateTimeFormatterBuilder()
            .appendText(
                ChronoField.MONTH_OF_YEAR,
                TextStyle.FULL_STANDALONE,
            )
            .appendLiteral(' ')
            .appendText(
                ChronoField.YEAR,
                TextStyle.FULL_STANDALONE,
            )
            .toFormatter(Locale.getDefault())
    }
    SketchesLazyGrid(
        modifier = modifier,
        state = state,
        overlayTop = overlayTop,
        overlayBottom = overlayBottom,
    ) {
        val size = files.size
        if (size > 0) {
            var index = 0
            var date = files[0].dateAdded
            while (index < size) {
                var groupSize = 0
                var nextDate = files[index].dateAdded
                while (date.year == nextDate.year && date.monthValue == nextDate.monthValue) {
                    groupSize++
                    if (index + groupSize > size - 1) {
                        break
                    }
                    nextDate = files[index + groupSize].dateAdded
                }
                val groupDate = date
                date = nextDate
                val groupOffset = index
                index += groupSize
                item(
                    key = SketchesMediaGridKey.Header(
                        year = groupDate.year,
                        month = groupDate.monthValue,
                    ),
                    contentType = SketchesMediaGridContentType.Header,
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    HeaderItem(
                        text = if (nowDate.year == groupDate.year) {
                            dateFormatterMonth.format(groupDate)
                        } else {
                            dateFormatterMonthYear.format(groupDate)
                        },
                        modifier = Modifier
                            .defaultAnimateItem()
                            .onSizeChanged { size ->
                                if (size != IntSize.Zero) {
                                    scrollSpec.headerItemSize = size
                                }
                            },
                    )
                }
                items(
                    count = groupSize,
                    key = { index -> SketchesMediaGridKey.Media(files[index + groupOffset].id) },
                    contentType = { SketchesMediaGridContentType.Media },
                ) { index ->
                    val file by rememberUpdatedState(files[index + groupOffset])
                    val fileSelected by remember {
                        derivedStateOf(structuralEqualityPolicy()) {
                            selectedFiles.contains(file.id)
                        }
                    }
                    MediaItem(
                        file = file,
                        fileSelected = fileSelected,
                        onLongClick = {
                            if (selectedFiles.isEmpty()) {
                                selectedFiles.add(file.id)
                            }
                        },
                        onClick = {
                            if (selectedFiles.isNotEmpty()) {
                                if (fileSelected) {
                                    selectedFiles.remove(file.id)
                                } else {
                                    selectedFiles.add(file.id)
                                }
                            } else {
                                onItemClick(index, file)
                            }
                        },
                        modifier = Modifier
                            .defaultAnimateItem()
                            .onSizeChanged { size ->
                                if (size != IntSize.Zero) {
                                    scrollSpec.mediaItemSize = size
                                }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderItem(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = text.capitalizeFirstChar(),
        modifier = modifier
            .background(
                color = colorScheme.background,
                shape = RectangleShape,
            )
            .padding(
                start = 4.dp,
                top = 8.dp,
                end = 4.dp,
                bottom = 0.dp,
            ),
        color = colorScheme.onBackground,
        fontSize = 16.sp,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
}

@Composable
private fun MediaItem(
    file: MediaFile,
    fileSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val file by rememberUpdatedState(file)
    val fileSelected by rememberUpdatedState(fileSelected)
    val colorScheme = MaterialTheme.colorScheme
    val dimens = LocalDimens.current
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1F)
            .border(
                width = dimens.mediaItemBorderThickness,
                color = if (fileSelected) {
                    colorScheme.onBackground.withLowTransparency()
                } else {
                    colorScheme.onBackground.withHighTransparency()
                },
                shape = RectangleShape,
            )
            .clipToBounds()
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    ) {
        val fileUri = file.uri
        SketchesThumbnailAsyncImage(
            uri = fileUri,
            contentDescription = stringResource(
                id = when (file.mediaType) {
                    MediaType.Image -> R.string.image
                    MediaType.Video -> R.string.video
                },
            ),
            modifier = Modifier.matchParentSize(),
        )
        if (fileSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = colorScheme.background.withMediumTransparency(),
                        shape = RectangleShape,
                    ),
            )
            Icon(
                painter = painterResource(R.drawable.ic_media_selected),
                contentDescription = stringResource(R.string.selected),
                tint = colorScheme.onBackground,
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .padding(all = dimens.mediaGridIconPadding)
                    .dropShadow(
                        shape = CircleShape,
                        shadow = Shadow(
                            radius = dimens.shadowBlurRadius,
                            color = colorScheme.background.withLowTransparency(),
                        ),
                    ),
            )
        }
        if (file.mediaType == MediaType.Video) {
            Icon(
                painter = painterResource(R.drawable.ic_video),
                contentDescription = stringResource(R.string.video),
                tint = colorScheme.onBackground,
                modifier = Modifier
                    .align(alignment = Alignment.BottomStart)
                    .padding(all = dimens.mediaGridIconPadding)
                    .dropShadow(
                        shape = CircleShape,
                        shadow = Shadow(
                            radius = dimens.shadowBlurRadius,
                            color = colorScheme.background.withLowTransparency(),
                        ),
                    ),
            )
        }
    }
}

/**
 * For [SketchesGroupingMediaGrid] only.
 * LazyGrid scroll is retarded as fuck.
 */
suspend fun LazyGridState.fastAnimateScrollToStart(files: Collection<MediaFile>) {
    scrollToItem(index = 0)
    /*var items = layoutInfo.visibleItemsInfo
    if (items.isEmpty()) {
        scrollToItem(index = 0)
        return
    }
    val itemSize = when (layoutInfo.orientation) {
        Orientation.Vertical -> items[0].size.height
        Orientation.Horizontal -> items[0].size.width
    }
    val viewportSize = when (layoutInfo.orientation) {
        Orientation.Vertical -> layoutInfo.viewportSize.height
        Orientation.Horizontal -> layoutInfo.viewportSize.width
    }
    val maxSpan = layoutInfo.maxSpan
    var jumpIndex = (maxSpan * viewportSize / itemSize)
    jumpIndex += jumpIndex % maxSpan
    if (firstVisibleItemIndex > jumpIndex) {
        scrollToItem(index = jumpIndex, scrollOffset = -itemSize)
    }
    items = layoutInfo.visibleItemsInfo
    if (items.isEmpty()) {
        scrollToItem(index = 0)
        return
    }
    val item = items.fastFirstOrNull { item -> item.offset.y >= layoutInfo.viewportStartOffset }
    if (item == null) {
        scrollToItem(index = 0)
        return
    }
    val itemOffset = when (layoutInfo.orientation) {
        Orientation.Vertical -> item.offset.y
        Orientation.Horizontal -> item.offset.x
    }
    animateScrollBy(
        value = -(item.row * itemSize - itemOffset).toFloat(),
        animationSpec = defaultAnimationSpec(),
    )*/
}

@OptIn(ExperimentalContracts::class)
inline fun calculateMediaIndexWithGroups(
    files: Collection<MediaFile>,
    predicate: (index: Int, file: MediaFile) -> Boolean,
): Int {
    contract { callsInPlace(predicate) }
    var offset = 0
    var fileIndex = -1
    var previousDate = LocalDateTime.MAX
    for ((index, file) in files.withIndex()) {
        val currentDate = file.dateAdded
        if (previousDate.year != currentDate.year || previousDate.monthValue != currentDate.monthValue) {
            offset++
        }
        if (predicate(index, file)) {
            fileIndex = index
            break
        }
        previousDate = currentDate
    }
    if (fileIndex == -1) {
        return -1
    }
    return fileIndex + offset
}

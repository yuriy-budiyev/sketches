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

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.min
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
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
import androidx.compose.ui.util.fastFirstOrNull
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaBucket
import com.github.yuriybudiyev.sketches.core.data.model.MediaFile
import com.github.yuriybudiyev.sketches.core.data.model.MediaType
import com.github.yuriybudiyev.sketches.core.text.capitalizeFirstChar
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultAnimateItem
import com.github.yuriybudiyev.sketches.core.ui.animation.defaultAnimationSpec
import com.github.yuriybudiyev.sketches.core.ui.colors.withHighTransparency
import com.github.yuriybudiyev.sketches.core.ui.colors.withLowTransparency
import com.github.yuriybudiyev.sketches.core.ui.colors.withMediumTransparency
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLazyGrid
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens
import com.github.yuriybudiyev.sketches.core.ui.utils.mainAxis

@Immutable
sealed interface SketchesMediaGridItemKey: Parcelable {

    @Parcelize
    @Immutable
    data class Date(
        val year: Int,
        val month: Int,
    ): SketchesMediaGridItemKey

    @Parcelize
    @Immutable
    data class File(
        val id: Long,
    ): SketchesMediaGridItemKey

    @Parcelize
    @Immutable
    data class Bucket(
        val id: Long,
    ): SketchesMediaGridItemKey
}

@Immutable
sealed interface SketchesMediaGridItemSpan: (LazyGridItemSpanScope) -> GridItemSpan {

    @Immutable
    data object Single: SketchesMediaGridItemSpan {

        override operator fun invoke(scope: LazyGridItemSpanScope): GridItemSpan =
            GridItemSpan(1)
    }

    @Immutable
    data object Line: SketchesMediaGridItemSpan {

        override operator fun invoke(scope: LazyGridItemSpanScope): GridItemSpan =
            GridItemSpan(scope.maxLineSpan)
    }
}

@Immutable
sealed interface SketchesMediaGridItemType {

    val span: SketchesMediaGridItemSpan

    @Immutable
    data object Date: SketchesMediaGridItemType {

        override val span: SketchesMediaGridItemSpan = SketchesMediaGridItemSpan.Line
    }

    @Immutable
    data object File: SketchesMediaGridItemType {

        override val span: SketchesMediaGridItemSpan = SketchesMediaGridItemSpan.Single
    }

    @Immutable
    data object Bucket: SketchesMediaGridItemType {

        override val span: SketchesMediaGridItemSpan = SketchesMediaGridItemSpan.Single
    }
}

@Immutable
sealed interface SketchesMediaGridItem {

    val key: SketchesMediaGridItemKey

    val type: SketchesMediaGridItemType

    @Immutable
    data class Date(
        val date: LocalDateTime,
    ): SketchesMediaGridItem {

        override val key: SketchesMediaGridItemKey = SketchesMediaGridItemKey.Date(date.year, date.monthValue)

        override val type: SketchesMediaGridItemType = SketchesMediaGridItemType.Date
    }

    @Immutable
    data class File(
        val file: MediaFile,
    ): SketchesMediaGridItem {

        override val key: SketchesMediaGridItemKey = SketchesMediaGridItemKey.File(file.id)

        override val type: SketchesMediaGridItemType = SketchesMediaGridItemType.File
    }

    @Immutable
    data class Bucket(
        val bucket: MediaBucket,
    ): SketchesMediaGridItem {

        override val key: SketchesMediaGridItemKey = SketchesMediaGridItemKey.Bucket(bucket.id)

        override val type: SketchesMediaGridItemType = SketchesMediaGridItemType.Bucket
    }
}

@Composable
fun rememberSketchesMediaGridState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): SketchesMediaGridState {
    val state = rememberLazyGridState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
    return rememberSaveable(state, saver = remember(state) { SketchesMediaGridStateImplSaver(state) }) {
        SketchesMediaGridStateImpl(state)
    }
}

@Stable
sealed interface SketchesMediaGridState {

    var items: List<SketchesMediaGridItem>

    val selectedItemKeys: SnapshotStateSet<SketchesMediaGridItemKey>

    suspend fun scrollToItem(
        index: Int,
        type: SketchesMediaGridItemType? = null,
        snapToClosestEdge: Boolean = false,
        onlyIfItemAtIndexIsNotVisible: Boolean = false,
    )

    suspend fun animateScrollToStart()
}

inline fun <reified T: SketchesMediaGridItem> SketchesMediaGridState.selectedItems(): List<T> {
    val selected = selectedItemKeys.toSet()
    val items = items
    val result = ArrayList<T>(min(selected.size, items.size))
    for (item in items) {
        if (item is T && selected.contains(item.key)) {
            result.add(item)
        }
    }
    return result
}

@Composable
fun SketchesMediaGrid(
    state: SketchesMediaGridState,
    onItemClick: (index: Int, item: SketchesMediaGridItem) -> Unit,
    modifier: Modifier = Modifier,
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
) {
    require(state is SketchesMediaGridStateImpl) { "Use rememberSketchesMediaGridState to obtain the state" }
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
        state = state.lazyGridState,
        overlayTop = overlayTop,
        overlayBottom = overlayBottom,
    ) {
        items(
            count = state.items.size,
            key = { index -> state.items[index].key },
            span = { index -> state.items[index].type.span(this) },
            contentType = { index -> state.items[index].type },
        ) { index ->
            val item = state.items[index]
            val itemSelected by remember(item.key) {
                computedStateOf { state.selectedItemKeys.contains(item.key) }
            }
            when (item) {
                is SketchesMediaGridItem.Date -> {
                    val date = item.date
                    val dateText = remember(date) {
                        if (nowDate.year == date.year) {
                            dateFormatterMonth.format(date)
                        } else {
                            dateFormatterMonthYear.format(date)
                        }
                    }
                    HeaderItem(
                        text = dateText,
                        modifier = Modifier
                            .defaultAnimateItem()
                            .onSizeChanged { size ->
                                state.dateItemSize = size
                            },
                    )
                }
                is SketchesMediaGridItem.File -> {
                    MediaFileItem(
                        file = item.file,
                        isSelected = itemSelected,
                        onLongClick = {
                            if (state.selectedItemKeys.isEmpty()) {
                                state.selectedItemKeys.add(item.key)
                            }
                        },
                        onClick = {
                            if (state.selectedItemKeys.isNotEmpty()) {
                                if (itemSelected) {
                                    state.selectedItemKeys.remove(item.key)
                                } else {
                                    state.selectedItemKeys.add(item.key)
                                }
                            } else {
                                onItemClick(index, item)
                            }
                        },
                        modifier = Modifier
                            .defaultAnimateItem()
                            .onSizeChanged { size ->
                                state.fileItemSize = size
                            },
                    )
                }
                is SketchesMediaGridItem.Bucket -> {
                    MediaBucketItem(
                        bucket = item.bucket,
                        isSelected = itemSelected,
                        onLongClick = {
                            if (state.selectedItemKeys.isEmpty()) {
                                state.selectedItemKeys.add(item.key)
                            }
                        },
                        onClick = {
                            if (state.selectedItemKeys.isNotEmpty()) {
                                if (itemSelected) {
                                    state.selectedItemKeys.remove(item.key)
                                } else {
                                    state.selectedItemKeys.add(item.key)
                                }
                            } else {
                                onItemClick(index, item)
                            }
                        },
                        modifier = Modifier
                            .defaultAnimateItem()
                            .onSizeChanged { size ->
                                state.bucketItemSize = size
                            },
                    )
                }
            }
        }
    }
}

private class SketchesMediaGridStateImpl(val lazyGridState: LazyGridState): SketchesMediaGridState {

    override var items: List<SketchesMediaGridItem>
        by mutableStateOf(emptyList(), referentialEqualityPolicy())

    override val selectedItemKeys: SnapshotStateSet<SketchesMediaGridItemKey> = SnapshotStateSet()

    override suspend fun scrollToItem(
        index: Int,
        type: SketchesMediaGridItemType?,
        snapToClosestEdge: Boolean,
        onlyIfItemAtIndexIsNotVisible: Boolean,
    ) {
        val itemsCount = items.size
        if (itemsCount == 0) {
            return
        }
        val layoutInfo = lazyGridState.layoutInfo
        if (layoutInfo.totalItemsCount != itemsCount) {
            return
        }
        val orientation = layoutInfo.orientation
        var index = index
        if (index < 0) {
            index = 0
        }
        if (index >= itemsCount) {
            index = itemsCount - 1
        }
        if (type != null) {
            for (index2 in 0..index) {
                if (items[index2].type != type) {
                    index++
                }
            }
            if (index >= itemsCount) {
                index = itemsCount - 1
            }
        }
        val visibleItemsInfo = layoutInfo.visibleItemsInfo
        if (onlyIfItemAtIndexIsNotVisible) {
            val visibleItem = visibleItemsInfo.fastFirstOrNull { info -> info.index == index }
            if (visibleItem != null) {
                val itemOffset = visibleItem.offset.mainAxis(orientation)
                val itemSize = visibleItem.size.mainAxis(orientation)
                val viewportStart = layoutInfo.viewportStartOffset
                val viewportEnd = layoutInfo.viewportEndOffset
                if (itemOffset >= viewportStart && itemOffset + itemSize <= viewportEnd) {
                    return
                }
            }
        }
        val item = items[index]
        var offset = 0
        if (snapToClosestEdge && visibleItemsInfo.isNotEmpty()) {
            val firstItem = visibleItemsInfo[0]
            val lastItem = visibleItemsInfo[visibleItemsInfo.lastIndex]
            if (firstItem !== lastItem && index > (lastItem.index - firstItem.index) / 2) {
                offset = layoutInfo.viewportEndOffset - itemSizeMainAxis(item.type, orientation)
            }
        }
        lazyGridState.scrollToItem(index, offset)
    }

    override suspend fun animateScrollToStart() {
        val items = items
        val itemsCount = items.size
        if (itemsCount == 0) {
            return
        }
        val firstVisibleItemIndex = lazyGridState.firstVisibleItemIndex
        val firstVisibleItemScrollOffset = lazyGridState.firstVisibleItemScrollOffset
        if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) {
            return
        }
        val layoutInfo = lazyGridState.layoutInfo
        if (layoutInfo.totalItemsCount != itemsCount) {
            return
        }
        val orientation = layoutInfo.orientation
        val scrollThreshold = layoutInfo.viewportSize.mainAxis(orientation) * 3 / 4
        val maxSpan = layoutInfo.maxSpan
        val itemSpacing = layoutInfo.mainAxisItemSpacing
        var itemIndex = 0
        var scrollAmount = 0
        var scrollIndex = 0
        scrollLoop@ while (itemIndex < itemsCount) {
            val groupType = items[itemIndex].type
            var nextGroupType = groupType
            var groupSize = 0
            groupLoop@ while (groupType == nextGroupType) {
                groupSize++
                val nextIndex = itemIndex + groupSize
                if (nextIndex >= itemsCount) {
                    break@groupLoop
                }
                nextGroupType = items[nextIndex].type
            }
            val itemSize = itemSizeMainAxis(groupType, orientation)
            when (groupType.span) {
                SketchesMediaGridItemSpan.Single -> {
                    repeat(groupSize % maxSpan) {
                        scrollAmount += itemSize + itemSpacing
                        scrollIndex += maxSpan
                        if (scrollAmount >= scrollThreshold || scrollIndex >= firstVisibleItemIndex) {
                            break@scrollLoop
                        }
                    }
                    val remainingItems = groupSize % maxSpan
                    if (remainingItems > 0) {
                        scrollAmount += itemSize + itemSpacing
                        scrollIndex += remainingItems
                        if (scrollAmount >= scrollThreshold || scrollIndex >= firstVisibleItemIndex) {
                            break@scrollLoop
                        }
                    }
                }
                SketchesMediaGridItemSpan.Line -> {
                    repeat(groupSize) {
                        scrollAmount += itemSize + itemSpacing
                        scrollIndex += 1
                        if (scrollAmount >= scrollThreshold || scrollIndex >= firstVisibleItemIndex) {
                            break@scrollLoop
                        }
                    }
                }
            }
            itemIndex += groupSize
        }
        if (firstVisibleItemIndex > scrollIndex) {
            scrollToItem(scrollIndex)
        } else {
            scrollAmount += firstVisibleItemScrollOffset
        }
        lazyGridState.animateScrollBy(-scrollAmount.toFloat(), defaultAnimationSpec())
    }

    private fun itemSizeMainAxis(type: SketchesMediaGridItemType, orientation: Orientation): Int =
        when (type) {
            SketchesMediaGridItemType.Date -> dateItemSize.mainAxis(orientation)
            SketchesMediaGridItemType.File -> fileItemSize.mainAxis(orientation)
            SketchesMediaGridItemType.Bucket -> bucketItemSize.mainAxis(orientation)
        }

    var dateItemSize: IntSize = IntSize.Zero
    var fileItemSize: IntSize = IntSize.Zero
    var bucketItemSize: IntSize = IntSize.Zero
}

@Parcelize
private data class SketchesMediaGridStateImplConfig(
    val dateItemWidth: Int,
    val dateItemHeight: Int,
    val fileItemWidth: Int,
    val fileItemHeight: Int,
    val bucketItemWidth: Int,
    val bucketItemHeight: Int,
    val selectedItems: ArrayList<SketchesMediaGridItemKey>,
): Parcelable

private class SketchesMediaGridStateImplSaver(private val state: LazyGridState):
    Saver<SketchesMediaGridStateImpl, SketchesMediaGridStateImplConfig> {

    override fun SaverScope.save(value: SketchesMediaGridStateImpl): SketchesMediaGridStateImplConfig =
        SketchesMediaGridStateImplConfig(
            dateItemWidth = value.dateItemSize.width,
            dateItemHeight = value.dateItemSize.height,
            fileItemWidth = value.fileItemSize.width,
            fileItemHeight = value.fileItemSize.height,
            bucketItemWidth = value.bucketItemSize.width,
            bucketItemHeight = value.bucketItemSize.height,
            selectedItems = value.selectedItemKeys.toSet().let { items ->
                items.mapTo(ArrayList(items.size)) { item -> item }
            },
        )

    override fun restore(value: SketchesMediaGridStateImplConfig): SketchesMediaGridStateImpl =
        SketchesMediaGridStateImpl(state).apply {
            dateItemSize = IntSize(value.dateItemWidth, value.dateItemHeight)
            fileItemSize = IntSize(value.fileItemWidth, value.fileItemHeight)
            bucketItemSize = IntSize(value.bucketItemWidth, value.bucketItemHeight)
            selectedItemKeys.addAll(value.selectedItems)
        }
}

@Composable
fun rememberSketchesMediaGridSpec(): SketchesMediaGridSpec =
    rememberSaveable(saver = SketchesMediaGridSpecSaver) { SketchesMediaGridSpec() }

@Stable
class SketchesMediaGridSpec {

    /**
     * Do not set manually.
     */
    var dateSize: IntSize by mutableStateOf(IntSize.Zero)

    /**
     * Do not set manually.
     */
    var fileSize: IntSize by mutableStateOf(IntSize.Zero)

    /**
     * Do not set manually.
     */
    var bucketSize: IntSize by mutableStateOf(IntSize.Zero)
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
            key = { index -> SketchesMediaGridItemKey.File(id = files[index].id) },
            contentType = { SketchesMediaGridItemType.File },
        ) { index ->
            val file by rememberUpdatedState(files[index])
            val fileSelected by remember { computedStateOf { selectedFiles.contains(file.id) } }
            MediaFileItem(
                file = file,
                isSelected = fileSelected,
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

@Composable
fun SketchesGroupingMediaGrid(
    files: List<MediaFile>,
    selectedFiles: SnapshotStateSet<Long>,
    onItemClick: (index: Int, file: MediaFile) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    spec: SketchesMediaGridSpec = rememberSketchesMediaGridSpec(),
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
                    key = SketchesMediaGridItemKey.Date(
                        year = groupDate.year,
                        month = groupDate.monthValue,
                    ),
                    contentType = SketchesMediaGridItemType.Date,
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
                                    spec.dateSize = size
                                }
                            },
                    )
                }
                items(
                    count = groupSize,
                    key = { index -> SketchesMediaGridItemKey.File(files[index + groupOffset].id) },
                    contentType = { SketchesMediaGridItemType.File },
                ) { index ->
                    val file by rememberUpdatedState(files[index + groupOffset])
                    val fileSelected by remember { computedStateOf { selectedFiles.contains(file.id) } }
                    MediaFileItem(
                        file = file,
                        isSelected = fileSelected,
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
                                    spec.fileSize = size
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
private fun MediaFileItem(
    file: MediaFile,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val dimens = LocalDimens.current
    Box(
        modifier = modifier
            .aspectRatio(ratio = 1F)
            .border(
                width = dimens.mediaItemBorderThickness,
                color = if (isSelected) {
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
        if (isSelected) {
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

@Composable
private fun MediaBucketItem(
    bucket: MediaBucket,
    isSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val dimens = LocalDimens.current
    Column(
        modifier = modifier
            .clipToBounds()
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 1F)
                .border(
                    width = dimens.mediaItemBorderThickness,
                    color = if (isSelected) {
                        colorScheme.onBackground.withLowTransparency()
                    } else {
                        colorScheme.onBackground.withHighTransparency()
                    },
                    shape = RectangleShape,
                )
                .clipToBounds(),
        ) {
            val coverUri = bucket.coverUri
            SketchesThumbnailAsyncImage(
                uri = coverUri,
                contentDescription = stringResource(R.string.bucket_cover),
                modifier = Modifier.matchParentSize(),
            )
            if (isSelected) {
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
            if (bucket.isHidden) {
                Icon(
                    painter = painterResource(R.drawable.ic_bucket_hidden),
                    contentDescription = stringResource(R.string.hidden_bucket),
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
        Text(
            text = bucket.name,
            modifier = Modifier.padding(
                start = 4.dp,
                top = 4.dp,
                end = 4.dp,
                bottom = 0.dp,
            ),
            color = colorScheme.onBackground,
            fontSize = 16.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        Text(
            text = bucket.size.toString(),
            modifier = Modifier.padding(
                start = 4.dp,
                top = 0.dp,
                end = 4.dp,
                bottom = 4.dp,
            ),
            color = colorScheme.onBackground,
            fontSize = 14.sp,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

/**
 * For [SketchesGroupingMediaGrid] only
 */
suspend fun LazyGridState.fastAnimateScrollToStart(
    spec: SketchesMediaGridSpec,
    files: List<MediaFile>,
) {
    val layoutInfo = layoutInfo
    if (layoutInfo.totalItemsCount == 0) {
        return
    }
    if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) {
        return
    }
    val headerItemSize = when (layoutInfo.orientation) {
        Orientation.Vertical -> spec.dateSize.height
        Orientation.Horizontal -> spec.dateSize.width
    }
    val mediaItemSize = when (layoutInfo.orientation) {
        Orientation.Vertical -> spec.fileSize.height
        Orientation.Horizontal -> spec.fileSize.width
    }
    val scrollThreshold = when (layoutInfo.orientation) {
        Orientation.Vertical -> layoutInfo.viewportSize.height
        Orientation.Horizontal -> layoutInfo.viewportSize.width
    } * 3 / 4
    val maxSpan = layoutInfo.maxSpan
    val itemSpacing = layoutInfo.mainAxisItemSpacing
    var scrollIndex = 0
    var scrollAmount = 0
    val filesSize = files.size
    if (filesSize == 0) {
        return
    }
    var fileIndex = 0
    var fileDate = files[0].dateAdded
    scrollLoop@ while (fileIndex < filesSize) {
        var groupSize = 0
        var nextDate = files[fileIndex].dateAdded
        while (fileDate.year == nextDate.year && fileDate.monthValue == nextDate.monthValue) {
            groupSize++
            if (fileIndex + groupSize > filesSize - 1) {
                break
            }
            nextDate = files[fileIndex + groupSize].dateAdded
        }
        if (scrollIndex >= firstVisibleItemIndex) {
            break@scrollLoop
        }
        scrollIndex++
        scrollAmount += headerItemSize + itemSpacing
        if (scrollIndex >= firstVisibleItemIndex) {
            break@scrollLoop
        }
        repeat(groupSize / maxSpan) {
            scrollAmount += mediaItemSize + itemSpacing
            scrollIndex += maxSpan
            if (scrollAmount >= scrollThreshold || scrollIndex >= firstVisibleItemIndex) {
                break@scrollLoop
            }
        }
        val remainder = groupSize % maxSpan
        if (remainder > 0) {
            scrollAmount += mediaItemSize + itemSpacing
            scrollIndex += remainder
        }
        if (scrollAmount >= scrollThreshold || scrollIndex >= firstVisibleItemIndex) {
            break@scrollLoop
        }
        fileDate = nextDate
        fileIndex += groupSize
    }
    if (firstVisibleItemIndex > scrollIndex) {
        scrollToItem(index = scrollIndex)
    } else {
        scrollAmount += firstVisibleItemScrollOffset
    }
    animateScrollBy(
        value = -scrollAmount.toFloat(),
        animationSpec = defaultAnimationSpec(),
    )
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

@Parcelize
private data class SketchesMediaGridSpecConfig(
    val dateWidth: Int,
    val dateHeight: Int,
    val fileWidth: Int,
    val fileHeight: Int,
    val bucketWidth: Int,
    val bucketHeight: Int,
): Parcelable

private object SketchesMediaGridSpecSaver: Saver<SketchesMediaGridSpec, SketchesMediaGridSpecConfig> {

    override fun SaverScope.save(value: SketchesMediaGridSpec): SketchesMediaGridSpecConfig =
        SketchesMediaGridSpecConfig(
            dateWidth = value.dateSize.width,
            dateHeight = value.dateSize.height,
            fileWidth = value.fileSize.width,
            fileHeight = value.fileSize.height,
            bucketWidth = value.bucketSize.width,
            bucketHeight = value.bucketSize.height,
        )

    override fun restore(value: SketchesMediaGridSpecConfig): SketchesMediaGridSpec =
        SketchesMediaGridSpec().apply {
            dateSize = IntSize(value.dateWidth, value.dateHeight)
            fileSize = IntSize(value.fileWidth, value.fileHeight)
            bucketSize = IntSize(value.bucketWidth, value.bucketHeight)
        }
}

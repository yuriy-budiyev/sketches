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
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.computedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
    data class Date(val year: Int, val month: Int): SketchesMediaGridItemKey

    @Parcelize
    @Immutable
    data class File(val id: Long): SketchesMediaGridItemKey

    @Parcelize
    @Immutable
    data class Bucket(val id: Long): SketchesMediaGridItemKey
}

@Immutable
sealed interface SketchesMediaGridItemContentType {

    @Immutable
    data object Date: SketchesMediaGridItemContentType

    @Immutable
    data object File: SketchesMediaGridItemContentType

    @Immutable
    data object Bucket: SketchesMediaGridItemContentType
}

@Composable
fun rememberSketchesMediaGridSpec(): SketchesMediaGridSpec =
    rememberSaveable(saver = SketchesMediaGridSpecSaver) { SketchesMediaGridSpec() }

@Stable
class SketchesMediaGridSpec {

    /**
     * Do not set manually.
     */
    var dateItemSize: IntSize by mutableStateOf(IntSize.Zero)

    /**
     * Do not set manually.
     */
    var mediaItemSize: IntSize by mutableStateOf(IntSize.Zero)
}

@Composable
fun SketchesMediaFilesGrid(
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
            contentType = { SketchesMediaGridItemContentType.File },
        ) { index ->
            val file by rememberUpdatedState(files[index])
            val fileSelected by remember { computedStateOf { selectedFiles.contains(file.id) } }
            MediaFileItem(
                uri = file.uri,
                mediaType = file.mediaType,
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
fun SketchesGroupingByMonthMediaFilesGrid(
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
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = SketchesMediaGridItemContentType.Date,
                ) {
                    DateItem(
                        date = groupDate.toLocalDate(),
                        modifier = Modifier
                            .defaultAnimateItem()
                            .onSizeChanged { size ->
                                if (size != IntSize.Zero) {
                                    spec.dateItemSize = size
                                }
                            },
                    )
                }
                items(
                    count = groupSize,
                    key = { index -> SketchesMediaGridItemKey.File(files[index + groupOffset].id) },
                    contentType = { SketchesMediaGridItemContentType.File },
                ) { index ->
                    val file by rememberUpdatedState(files[index + groupOffset])
                    val fileSelected by remember { computedStateOf { selectedFiles.contains(file.id) } }
                    MediaFileItem(
                        uri = file.uri,
                        mediaType = file.mediaType,
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
                                    spec.mediaItemSize = size
                                }
                            },
                    )
                }
            }
        }
    }
}

@Composable
fun SketchesMediaBucketsGrid(
    buckets: List<MediaBucket>,
    selectedBuckets: SnapshotStateSet<Long>,
    onItemClick: (index: Int, file: MediaBucket) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
) {
    val buckets by rememberUpdatedState(buckets)
    val selectedBuckets by rememberUpdatedState(selectedBuckets)
    val onItemClick by rememberUpdatedState(onItemClick)
    SketchesLazyGrid(
        modifier = modifier,
        state = state,
        overlayTop = overlayTop,
        overlayBottom = overlayBottom,
    ) {
        items(
            count = buckets.size,
            key = { index -> SketchesMediaGridItemKey.Bucket(id = buckets[index].id) },
            contentType = { SketchesMediaGridItemContentType.Bucket },
        ) { index ->
            val bucket by rememberUpdatedState(buckets[index])
            val bucketSelected by remember { computedStateOf { selectedBuckets.contains(bucket.id) } }
            MediaBucketItem(
                name = bucket.name,
                size = bucket.size,
                coverUri = bucket.coverUri,
                isHidden = bucket.isHidden,
                isSelected = bucketSelected,
                onLongClick = {
                    if (selectedBuckets.isEmpty()) {
                        selectedBuckets.add(bucket.id)
                    }
                },
                onClick = {
                    if (selectedBuckets.isNotEmpty()) {
                        if (bucketSelected) {
                            selectedBuckets.remove(bucket.id)
                        } else {
                            selectedBuckets.add(bucket.id)
                        }
                    } else {
                        onItemClick(index, bucket)
                    }
                },
                modifier = Modifier.defaultAnimateItem(),
            )
        }
    }
}

@Composable
private fun DateItem(
    date: LocalDate,
    modifier: Modifier = Modifier,
) {
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
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = if (nowDate.year == date.year) {
            dateFormatterMonth.format(date).capitalizeFirstChar()
        } else {
            dateFormatterMonthYear.format(date).capitalizeFirstChar()
        },
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
    uri: String,
    mediaType: MediaType,
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
        SketchesThumbnailAsyncImage(
            uri = uri,
            contentDescription = stringResource(
                id = when (mediaType) {
                    MediaType.Image -> R.string.image
                    MediaType.Video -> R.string.video
                },
            ),
            modifier = Modifier.matchParentSize(),
        )
        if (isSelected) {
            ItemSelectionMarker(modifier = Modifier.matchParentSize())
        }
        if (mediaType == MediaType.Video) {
            ItemIcon(
                iconRes = R.drawable.ic_video,
                descriptionRes = R.string.video,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}

@Composable
private fun MediaBucketItem(
    name: String,
    size: Int,
    coverUri: String,
    isHidden: Boolean,
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
            SketchesThumbnailAsyncImage(
                uri = coverUri,
                contentDescription = stringResource(R.string.bucket_cover),
                modifier = Modifier.matchParentSize(),
            )
            if (isSelected) {
                ItemSelectionMarker(modifier = Modifier.matchParentSize())
            }
            if (isHidden) {
                ItemIcon(
                    iconRes = R.drawable.ic_bucket_hidden,
                    descriptionRes = R.string.hidden_bucket,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
        Text(
            text = name,
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
            text = size.toString(),
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

@Composable
private fun ItemSelectionMarker(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = colorScheme.background.withMediumTransparency(),
                    shape = RectangleShape,
                ),
        )
        ItemIcon(
            iconRes = R.drawable.ic_media_selected,
            descriptionRes = R.string.selected,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

@Composable
private fun ItemIcon(
    @DrawableRes
    iconRes: Int,
    @StringRes
    descriptionRes: Int,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val dimens = LocalDimens.current
    Icon(
        painter = painterResource(iconRes),
        contentDescription = stringResource(descriptionRes),
        tint = colorScheme.onBackground,
        modifier = modifier
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

/**
 * For [SketchesGroupingByMonthMediaFilesGrid] only
 */
suspend fun LazyGridState.fastAnimateScrollToStart(
    spec: SketchesMediaGridSpec,
    files: List<MediaFile>,
) {
    if (layoutInfo.totalItemsCount == 0) {
        return
    }
    if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) {
        return
    }
    val dateItemSize = spec.dateItemSize.mainAxis(layoutInfo.orientation)
    val mediaItemSize = spec.mediaItemSize.mainAxis(layoutInfo.orientation)
    val scrollThreshold = layoutInfo.viewportSize.mainAxis(layoutInfo.orientation) * 3 / 4
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
        scrollAmount += dateItemSize + itemSpacing
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
inline fun calculateMediaIndexWithMonthGroups(
    files: Collection<MediaFile>,
    predicate: (index: Int, file: MediaFile) -> Boolean,
): Int {
    contract {
        callsInPlace(predicate, InvocationKind.UNKNOWN)
    }
    var offset = 0
    var fileIndex = -1
    var previousDate = LocalDateTime.MAX
    for ([index, file] in files.withIndex()) {
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
    val dateItemWidth: Int,
    val dateItemHeight: Int,
    val mediaItemWidth: Int,
    val mediaItemHeight: Int,
): Parcelable

private object SketchesMediaGridSpecSaver: Saver<SketchesMediaGridSpec, SketchesMediaGridSpecConfig> {

    override fun SaverScope.save(value: SketchesMediaGridSpec): SketchesMediaGridSpecConfig =
        SketchesMediaGridSpecConfig(
            dateItemWidth = value.dateItemSize.width,
            dateItemHeight = value.dateItemSize.height,
            mediaItemWidth = value.mediaItemSize.width,
            mediaItemHeight = value.mediaItemSize.height,
        )

    override fun restore(value: SketchesMediaGridSpecConfig): SketchesMediaGridSpec =
        SketchesMediaGridSpec().apply {
            dateItemSize = IntSize(value.dateItemWidth, value.dateItemHeight)
            mediaItemSize = IntSize(value.mediaItemWidth, value.mediaItemHeight)
        }
}

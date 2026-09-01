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
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import com.github.yuriybudiyev.sketches.core.text.capitalizeFirstChar
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesLazyGrid
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens
import com.github.yuriybudiyev.sketches.core.ui.theme.withHighTransparency
import com.github.yuriybudiyev.sketches.core.ui.theme.withLowTransparency
import com.github.yuriybudiyev.sketches.core.ui.theme.withMediumTransparency
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.YearMonth
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
    data class GroupHeader(
        val year: Int,
        val month: Int,
    ): SketchesMediaGridKey

    @Parcelize
    @Immutable
    data class MediaStoreFile(
        val fileId: Long,
    ): SketchesMediaGridKey
}

@Immutable
sealed interface SketchesMediaGridContentType {

    @Immutable
    data object GroupHeader: SketchesMediaGridContentType

    @Immutable
    data object MediaStoreFile: SketchesMediaGridContentType
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
            key = { index -> SketchesMediaGridKey.MediaStoreFile(fileId = files[index].id) },
            contentType = { SketchesMediaGridContentType.MediaStoreFile },
        ) { index ->
            val file by rememberUpdatedState(files[index])
            val fileSelected by remember {
                derivedStateOf(structuralEqualityPolicy()) {
                    selectedFiles.contains(file.id)
                }
            }
            SketchesMediaGridItem(
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
                        onItemClick(
                            index,
                            file,
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun SketchesGroupingMediaGrid(
    items: Map<YearMonth, List<MediaFile>>,
    selectedFiles: SnapshotStateSet<Long>,
    onItemClick: (index: Int, file: MediaFile) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
) {
    val items by rememberUpdatedState(items)
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
    val colorScheme = MaterialTheme.colorScheme
    SketchesLazyGrid(
        modifier = modifier,
        state = state,
        overlayTop = overlayTop,
        overlayBottom = overlayBottom,
    ) {
        for ((month, files) in items) {
            item(
                key = SketchesMediaGridKey.GroupHeader(
                    year = month.year,
                    month = month.monthValue,
                ),
                contentType = SketchesMediaGridContentType.GroupHeader,
                span = { GridItemSpan(maxLineSpan) },
            ) {
                val text = if (nowDate.year == month.year) {
                    dateFormatterMonth.format(month)
                } else {
                    dateFormatterMonthYear.format(month)
                }
                Text(
                    text = text.capitalizeFirstChar(),
                    modifier = Modifier
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
            items(
                count = files.size,
                key = { index -> SketchesMediaGridKey.MediaStoreFile(files[index].id) },
                contentType = { SketchesMediaGridContentType.MediaStoreFile },
            ) { index ->
                val file by rememberUpdatedState(files[index])
                val fileSelected by remember {
                    derivedStateOf(structuralEqualityPolicy()) {
                        selectedFiles.contains(file.id)
                    }
                }
                SketchesMediaGridItem(
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
                            onItemClick(
                                index,
                                file,
                            )
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun calculateMediaIndexWithGroups(
    files: Collection<MediaFile>,
    predicate: (index: Int, file: MediaFile) -> Boolean,
): Int {
    contract { callsInPlace(predicate) }
    var offset = 0
    var fileIndex = -1
    var previousDate = LocalDate.MAX
    for ((index, file) in files.withIndex()) {
        val currentDate = file.dateAdded.toLocalDate()
        if (previousDate.year != currentDate.year || previousDate.monthValue != currentDate.monthValue) {
            offset++
        }
        if (
            predicate(
                index,
                file,
            )
        ) {
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

@Composable
private fun LazyGridItemScope.SketchesMediaGridItem(
    file: MediaFile,
    fileSelected: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    val file by rememberUpdatedState(file)
    val fileSelected by rememberUpdatedState(fileSelected)
    val colorScheme = MaterialTheme.colorScheme
    val dimens = LocalDimens.current
    Box(
        modifier = Modifier
            .animateItem()
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

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import com.github.yuriybudiyev.sketches.core.text.capitalizeFirstCharacter
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.dimens.SketchesDimens
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale

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

sealed interface SketchesMediaGridContentType {

    @Immutable
    data object GroupHeader: SketchesMediaGridContentType

    @Immutable
    data object MediaStoreFile: SketchesMediaGridContentType
}

@Composable
fun SketchesMediaGrid(
    files: List<MediaStoreFile>,
    selectedFiles: SnapshotStateSet<Long>,
    onItemClick: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
) {
    val filesUpdated by rememberUpdatedState(files)
    val selectedFilesUpdated by rememberUpdatedState(selectedFiles)
    val onItemClickUpdated by rememberUpdatedState(onItemClick)
    SketchesLazyGrid(
        modifier = modifier,
        state = state,
        overlayTop = overlayTop,
        overlayBottom = overlayBottom,
    ) {
        items(
            count = filesUpdated.size,
            key = { index -> SketchesMediaGridKey.MediaStoreFile(fileId = filesUpdated[index].id) },
            contentType = { SketchesMediaGridContentType.MediaStoreFile },
        ) { index ->
            val file = filesUpdated[index]
            SketchesMediaGridItem(
                file = file,
                selectedFilesUpdated.contains(file.id),
                modifier = Modifier.selectable(
                    file = { file },
                    filesIds = { filesUpdated.map { file -> file.id } },
                    selectedFiles = { selectedFilesUpdated },
                    onClick = {
                        onItemClickUpdated(
                            index,
                            file,
                        )
                    },
                )
            )
        }
    }
}

@Composable
fun SketchesGroupingMediaGrid(
    items: Map<YearMonth, List<MediaStoreFile>>,
    selectedFiles: SnapshotStateSet<Long>,
    onItemClick: (index: Int, file: MediaStoreFile) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    overlayTop: Boolean = false,
    overlayBottom: Boolean = false,
) {
    val itemsUpdated by rememberUpdatedState(items)
    val selectedFilesUpdated by rememberUpdatedState(selectedFiles)
    val onItemClickUpdated by rememberUpdatedState(onItemClick)
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
        var offset = 0
        for ((month, files) in itemsUpdated) {
            val localOffset = offset
            offset += files.size
            item(
                key = SketchesMediaGridKey.GroupHeader(
                    year = month.year,
                    month = month.monthValue,
                ),
                contentType = SketchesMediaGridContentType.GroupHeader,
                span = { GridItemSpan(maxLineSpan) },
            ) {
                val text = if (nowDate.year == month.year) {
                    month.format(dateFormatterMonth)
                } else {
                    month.format(dateFormatterMonthYear)
                }
                Text(
                    text = text.capitalizeFirstCharacter(),
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RectangleShape,
                        )
                        .padding(
                            start = 4.dp,
                            top = 8.dp,
                            end = 4.dp,
                            bottom = 0.dp,
                        ),
                    color = MaterialTheme.colorScheme.onBackground,
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
                val file = files[index]
                SketchesMediaGridItem(
                    file = file,
                    selectedFilesUpdated.contains(file.id),
                    modifier = Modifier.selectable(
                        file = { file },
                        filesIds = {
                            itemsUpdated
                                .asSequence()
                                .flatMap { (_, files) -> files }
                                .map { file -> file.id }
                                .toList()
                        },
                        selectedFiles = { selectedFilesUpdated },
                        onClick = {
                            onItemClickUpdated(
                                index + localOffset,
                                file,
                            )
                        },
                    )
                )
            }
        }
    }
}

fun calculateMediaIndexWithGroups(
    fileIndex: Int,
    files: Collection<MediaStoreFile>,
): Int {
    var offset = 0
    var previousDate = LocalDate.MAX
    for ((index, file) in files.withIndex()) {
        val currentDate = file.dateAdded.toLocalDate()
        if (previousDate.year != currentDate.year || previousDate.monthValue != currentDate.monthValue) {
            offset++
        }
        if (index == fileIndex) {
            break
        }
        previousDate = currentDate
    }
    return fileIndex + offset
}

@Composable
private fun SketchesMediaGridItem(
    file: MediaStoreFile,
    fileSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val fileUpdated by rememberUpdatedState(file)
    val fileSelectedUpdated by rememberUpdatedState(fileSelected)
    Box(
        modifier = Modifier
            .aspectRatio(ratio = 1f)
            .border(
                width = SketchesDimens.MediaItemBorderThickness,
                color = if (fileSelectedUpdated) {
                    MaterialTheme.colorScheme.onBackground
                        .copy(alpha = SketchesColors.UiAlphaLowTransparency)
                } else {
                    MaterialTheme.colorScheme.onBackground
                        .copy(alpha = SketchesColors.UiAlphaHighTransparency)
                },
                shape = MaterialTheme.shapes.extraSmall,
            )
            .clip(shape = MaterialTheme.shapes.extraSmall)
            .then(modifier),
    ) {
        SketchesAsyncImage(
            uri = fileUpdated.uri,
            contentDescription = stringResource(
                id = when (fileUpdated.mediaType) {
                    MediaType.Image -> R.string.image
                    MediaType.Video -> R.string.video
                },
            ),
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Low,
            enableLoadingIndicator = true,
            enableErrorIndicator = true,
        )
        if (fileSelectedUpdated) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = MaterialTheme.colorScheme.background
                            .copy(alpha = SketchesColors.UiAlphaHighTransparency),
                    ),
            )
        }
        if (fileUpdated.mediaType == MediaType.Video) {
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.BottomStart)
                    .padding(all = SketchesDimens.MediaGridVideoIconPadding)
                    .let { modifier ->
                        if (!fileSelectedUpdated) {
                            modifier.background(
                                color = MaterialTheme.colorScheme.background
                                    .copy(alpha = SketchesColors.UiAlphaHighTransparency),
                                shape = CircleShape,
                            )
                        } else {
                            modifier
                        }
                    },
            ) {
                Icon(
                    imageVector = SketchesIcons.Video,
                    contentDescription = stringResource(R.string.video),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

private inline fun Modifier.selectable(
    crossinline file: () -> MediaStoreFile,
    crossinline filesIds: () -> Collection<Long>,
    crossinline selectedFiles: () -> SnapshotStateSet<Long>,
    crossinline onClick: () -> Unit,
): Modifier =
    combinedClickable(
        onLongClick = {
            val file = file()
            val selectedFiles = selectedFiles()
            if (selectedFiles.isEmpty()) {
                selectedFiles.add(file.id)
            } else {
                if (selectedFiles.contains(file.id)) {
                    selectedFiles.clear()
                } else {
                    selectedFiles.addAll(filesIds())
                }
            }
        },
        onClick = {
            val selectedFiles = selectedFiles()
            if (selectedFiles.isNotEmpty()) {
                val file = file()
                if (selectedFiles.contains(file.id)) {
                    selectedFiles.remove(file.id)
                } else {
                    selectedFiles.add(file.id)
                }
            } else {
                onClick()
            }
        },
    )

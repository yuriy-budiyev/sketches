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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale

@Composable
fun SketchesMediaGrid(
    files: ImmutableList<MediaStoreFile>,
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
            key = { index -> MediaStoreFileKey(fileId = filesUpdated[index].id) },
            contentType = { index -> MediaStoreFileContentType(mediaType = filesUpdated[index].mediaType) },
        ) { index ->
            val file = filesUpdated[index]
            SketchesMediaGridItem(
                file = file,
                files = filesUpdated,
                selectedFiles = selectedFilesUpdated,
                onClick = {
                    onItemClickUpdated(
                        index,
                        file
                    )
                },
            )
        }
    }
}

@Composable
fun SketchesGroupingMediaGrid(
    files: ImmutableList<MediaStoreFile>,
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
    var previousDate by remember { mutableStateOf(LocalDate.MAX) }
    val nowDate = remember { LocalDate.now() }
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
    val dateFormatterMonth = remember {
        DateTimeFormatterBuilder()
            .appendText(
                ChronoField.MONTH_OF_YEAR,
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
        for ((index, file) in filesUpdated.withIndex()) {
            val currentDate = file.dateAdded.toLocalDate()
            if (previousDate.year != currentDate.year || previousDate.monthValue != currentDate.monthValue) {
                item(
                    key = GroupHeaderKey(
                        year = currentDate.year,
                        month = currentDate.monthValue,
                    ),
                    contentType = GroupHeaderContentType,
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    val text = if (nowDate.year == currentDate.year) {
                        currentDate.format(dateFormatterMonth)
                    } else {
                        currentDate.format(dateFormatterMonthYear)
                    }
                    Text(
                        text = text.capitalizeFirstCharacter(),
                        modifier = Modifier.padding(
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
                previousDate = currentDate
            }
            item(
                key = MediaStoreFileKey(fileId = file.id),
                contentType = MediaStoreFileContentType(mediaType = file.mediaType),
            ) {
                SketchesMediaGridItem(
                    file = file,
                    files = filesUpdated,
                    selectedFiles = selectedFilesUpdated,
                    onClick = {
                        onItemClickUpdated(
                            index,
                            file
                        )
                    }
                )
            }
        }
    }
}

fun calculateMediaIndexWithGroups(
    index: Int,
    files: Collection<MediaStoreFile>,
): Int {
    var offset = 0
    var previousDate = LocalDate.MAX
    for ((fileIndex, file) in files.withIndex()) {
        val currentDate = file.dateAdded.toLocalDate()
        if (previousDate.year != currentDate.year || previousDate.monthValue != currentDate.monthValue) {
            offset++
        }
        if (fileIndex == index) {
            break
        }
        previousDate = currentDate
    }
    return index + offset
}

@Composable
private fun SketchesMediaGridItem(
    file: MediaStoreFile,
    files: ImmutableList<MediaStoreFile>,
    selectedFiles: SnapshotStateSet<Long>,
    onClick: () -> Unit,
) {
    val fileUpdated by rememberUpdatedState(file)
    val filesUpdated by rememberUpdatedState(files)
    val selectedFilesUpdated by rememberUpdatedState(selectedFiles)
    val onClickUpdated by rememberUpdatedState(onClick)
    val fileSelectedUpdated by rememberUpdatedState(selectedFilesUpdated.contains(fileUpdated.id))
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
            .combinedClickable(
                onLongClick = {
                    if (selectedFilesUpdated.isEmpty()) {
                        selectedFilesUpdated.add(fileUpdated.id)
                    } else {
                        if (selectedFilesUpdated.contains(fileUpdated.id)) {
                            selectedFilesUpdated.clear()
                        } else {
                            selectedFilesUpdated.addAll(filesUpdated.map { file -> file.id })
                        }
                    }
                },
                onClick = {
                    if (selectedFilesUpdated.isNotEmpty()) {
                        if (selectedFilesUpdated.contains(fileUpdated.id)) {
                            selectedFilesUpdated.remove(fileUpdated.id)
                        } else {
                            selectedFilesUpdated.add(fileUpdated.id)
                        }
                    } else {
                        onClickUpdated()
                    }
                },
            ),
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

@Immutable
@Parcelize
private data class GroupHeaderKey(
    val year: Int,
    val month: Int,
): Parcelable

@Immutable
private data object GroupHeaderContentType

@Immutable
@Parcelize
private data class MediaStoreFileKey(val fileId: Long): Parcelable

@Immutable
private data class MediaStoreFileContentType(val mediaType: MediaType)

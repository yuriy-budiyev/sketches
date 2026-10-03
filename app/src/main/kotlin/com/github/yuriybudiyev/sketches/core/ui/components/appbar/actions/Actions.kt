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

package com.github.yuriybudiyev.sketches.core.ui.components.appbar.actions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.data.model.MediaFile
import com.github.yuriybudiyev.sketches.core.ui.components.SketchesActionButton
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.BatchAction
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.MediaBatchState
import com.github.yuriybudiyev.sketches.core.ui.components.media.share.prepareForSharing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SelectAction(
    selected: Boolean,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
) {
    SketchesActionButton(
        icon = painterResource(
            if (selected) {
                R.drawable.ic_select_none
            } else {
                R.drawable.ic_select_all
            },
        ),
        hint = stringResource(
            if (selected) {
                R.string.select_none
            } else {
                R.string.select_all
            },
        ),
        onClick = {
            if (selected) {
                onSelectNone()
            } else {
                onSelectAll()
            }
        },
    )
}

@Composable
fun DeleteAction(
    @DrawableRes
    iconRes: Int = R.drawable.ic_delete,
    @StringRes
    hintRes: Int = R.string.delete_selected,
    onDelete: () -> Unit,
) {
    SketchesActionButton(
        icon = painterResource(iconRes),
        hint = stringResource(hintRes),
        onClick = onDelete,
    )
}

@Composable
fun ShareAction(
    allFiles: List<MediaFile>,
    selectedFiles: SnapshotStateSet<Long>,
    mediaBatchState: MediaBatchState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val allFiles by rememberUpdatedState(allFiles)
    val selectedFiles by rememberUpdatedState(selectedFiles)
    val mediaBatchState by rememberUpdatedState(mediaBatchState)
    val shareTitle = stringResource(R.string.share_selected)
    SketchesActionButton(
        icon = painterResource(R.drawable.ic_share),
        hint = shareTitle,
        onClick = {
            coroutineScope.launch {
                allFiles.prepareForSharing(
                    filterIds = selectedFiles.toSet(),
                    mediaSizeLimit = MediaBatchState.BatchSize,
                    onDataReady = { media, mimeType ->
                        mediaBatchState.start(
                            media = media,
                            payload = BatchAction.Share(
                                chooserTitle = shareTitle,
                                mimeType = mimeType,
                            ),
                        )
                    },
                )
            }
        },
    )
}

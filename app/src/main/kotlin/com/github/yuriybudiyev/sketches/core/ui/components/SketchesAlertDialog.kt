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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.ui.wsc.LocalWindowSizeClass

@Composable
fun SketchesAlertDialog(
    titleText: String,
    contentText: String,
    positiveButtonText: String,
    negativeButtonText: String,
    onPositiveResult: () -> Unit,
    onNegativeResult: () -> Unit,
) {
    val windowSizeClass by rememberUpdatedState(LocalWindowSizeClass.current)
    AlertDialog(
        title = {
            Text(
                text = titleText,
                fontSize = 24.sp,
            )
        },
        text = {
            Text(
                text = contentText,
                fontSize = 18.sp,
            )
        },
        confirmButton = {
            SketchesFilledButton(
                text = positiveButtonText,
                onClick = onPositiveResult,
            )
        },
        dismissButton = {
            SketchesOutlinedButton(
                text = negativeButtonText,
                onClick = onNegativeResult,
            )
        },
        onDismissRequest = onNegativeResult,
        modifier = when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Compact -> Modifier.fillMaxWidth()
            else -> Modifier.wrapContentWidth()
        },
    )
}

@Composable
@NonRestartableComposable
fun SketchesDeleteImagesConfirmationDialog(
    count: Int,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    SketchesAlertDialog(
        titleText = stringResource(R.string.delete_images_dialog_title),
        contentText = if (count == 1) {
            stringResource(R.string.delete_images_dialog_content_one)
        } else {
            pluralStringResource(
                id = R.plurals.delete_images_dialog_content,
                count = count,
                formatArgs = arrayOf(count),
            )
        },
        positiveButtonText = stringResource(R.string.delete_images_dialog_positive),
        negativeButtonText = stringResource(R.string.delete_images_dialog_negative),
        onPositiveResult = onDelete,
        onNegativeResult = onDismiss,
    )
}

@Composable
@NonRestartableComposable
fun SketchesDeleteBookmarksConfirmationDialog(
    count: Int,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    SketchesAlertDialog(
        titleText = stringResource(R.string.delete_bookmarks_dialog_title),
        contentText = if (count == 1) {
            stringResource(R.string.delete_bookmarks_dialog_content_one)
        } else {
            pluralStringResource(
                id = R.plurals.delete_bookmarks_dialog_content,
                count = count,
                formatArgs = arrayOf(count),
            )
        },
        positiveButtonText = stringResource(R.string.delete_bookmarks_dialog_positive),
        negativeButtonText = stringResource(R.string.delete_bookmarks_dialog_negative),
        onPositiveResult = onDelete,
        onNegativeResult = onDismiss,
    )
}

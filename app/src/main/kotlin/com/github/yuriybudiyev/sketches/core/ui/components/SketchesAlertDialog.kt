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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.github.yuriybudiyev.sketches.R

@Composable
fun SketchesAlertDialog(
    titleText: String,
    contentText: String,
    positiveButtonText: String,
    negativeButtonText: String,
    onPositiveResult: () -> Unit,
    onNegativeResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        title = {
            Text(
                text = titleText,
            )
        },
        text = {
            Text(
                text = contentText,
                fontSize = 18.sp,
            )
        },
        onDismissRequest = onNegativeResult,
        confirmButton = {
            TextButton(onClick = onPositiveResult) {
                Text(
                    text = positiveButtonText,
                    fontSize = 16.sp,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onNegativeResult) {
                Text(
                    text = negativeButtonText,
                    fontSize = 16.sp,
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
@NonRestartableComposable
fun SketchesDeleteConfirmationDialog(
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SketchesAlertDialog(
        titleText = stringResource(R.string.delete_image_dialog_title),
        contentText = stringResource(R.string.delete_image_dialog_content),
        positiveButtonText = stringResource(R.string.delete_image_dialog_positive),
        negativeButtonText = stringResource(R.string.delete_image_dialog_negative),
        onPositiveResult = onDelete,
        onNegativeResult = onDismiss,
        modifier = modifier,
    )
}

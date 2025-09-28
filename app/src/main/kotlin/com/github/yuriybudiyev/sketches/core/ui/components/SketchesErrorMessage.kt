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

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.yuriybudiyev.sketches.BuildConfig
import com.github.yuriybudiyev.sketches.R
import kotlinx.coroutines.launch

@Composable
fun SketchesErrorMessage(
    thrown: Throwable,
    modifier: Modifier = Modifier,
) {
    if (BuildConfig.DEBUG) {
        val clipboardUpdated by rememberUpdatedState(LocalClipboard.current)
        val thrownUpdated by rememberUpdatedState(thrown)
        val coroutineScope = rememberCoroutineScope()
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SketchesMessage(
                text = stringResource(R.string.unexpected_error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            SketchesMessage(
                text = thrownUpdated.toString(),
                modifier = Modifier
                    .clickable {
                        coroutineScope.launch {
                            clipboardUpdated.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText(
                                        "Stack trace",
                                        thrownUpdated.stackTraceToString()
                                    )
                                )
                            )
                        }
                    }
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    } else {
        val message = thrown.message
        SketchesCenteredMessage(
            text = if (message.isNullOrEmpty()) {
                stringResource(R.string.unexpected_error)
            } else {
                stringResource(R.string.unexpected_error) + "\n${thrown.message}"
            },
            modifier = modifier,
        )
    }
}

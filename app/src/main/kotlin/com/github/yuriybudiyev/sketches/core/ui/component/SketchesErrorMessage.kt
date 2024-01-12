/*
 * MIT License
 *
 * Copyright (c) 2023 Yuriy Budiyev
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

package com.github.yuriybudiyev.sketches.core.ui.component

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.github.yuriybudiyev.sketches.BuildConfig
import com.github.yuriybudiyev.sketches.R

@Composable
fun SketchesErrorMessage(
    thrown: Throwable,
    modifier: Modifier = Modifier,
) {
    if (BuildConfig.DEBUG) {
        val contextUpdated by rememberUpdatedState(LocalContext.current)
        val clipboardManager by rememberUpdatedState(LocalClipboardManager.current)
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SketchesMessage(
                text = stringResource(id = R.string.unexpected_error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            SketchesMessage(
                text = thrown.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable {
                        clipboardManager.setText(
                            AnnotatedString
                                .Builder(thrown.stackTraceToString())
                                .toAnnotatedString(),
                        )
                        Toast
                            .makeText(
                                contextUpdated,
                                contextUpdated.getString(R.string.stack_trace_copied),
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    },
            )
        }
    } else {
        SketchesCenteredMessage(
            text = stringResource(id = R.string.unexpected_error),
            modifier = modifier,
        )
    }
}

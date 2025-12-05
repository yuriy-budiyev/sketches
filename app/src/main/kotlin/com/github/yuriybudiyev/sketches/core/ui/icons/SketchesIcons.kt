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

package com.github.yuriybudiyev.sketches.core.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Deselect
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share

object SketchesIcons {

    val ImageLoading = Icons.Rounded.Image
    val ImageError = Icons.Rounded.BrokenImage
    val SelectAll = Icons.Rounded.SelectAll
    val SelectNone = Icons.Rounded.Deselect
    val Share = Icons.Rounded.Share
    val Delete = Icons.Rounded.DeleteForever
    val Video = Icons.Rounded.PlayCircleOutline
    val Play = Icons.Rounded.PlayArrow
    val Pause = Icons.Rounded.Pause
    val VolumeEnabled = Icons.AutoMirrored.Rounded.VolumeUp
    val VolumeDisabled = Icons.AutoMirrored.Rounded.VolumeOff
    val UpdateMediaSelection = Icons.Rounded.PermMedia
    val MediaSelected = Icons.Rounded.CheckCircleOutline
}

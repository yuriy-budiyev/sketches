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

package com.github.yuriybudiyev.sketches.core.ui.component.player

import android.net.Uri
import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import com.github.yuriybudiyev.sketches.core.ui.effect.LifecycleEventEffect

@Composable
fun MediaPlayer(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context by rememberUpdatedState(LocalContext.current)
    val state = rememberMediaPlayerState(context)
    val displayAspectRatio by state.videoDisplayAspectRatioState
    Box(modifier = modifier) {
        AndroidView(modifier = Modifier
            .fillMaxSize()
            .aspectRatio(
                ratio = displayAspectRatio,
                matchHeightConstraintsFirst = displayAspectRatio < 1f
            )
            .align(Alignment.Center),
            factory = { SurfaceView(context) },
            update = { view ->
                state.setSurfaceView(view)
            })
    }
    LaunchedEffect(uri) {
        state.open(uri)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        state.play()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        state.pause()
    }
}

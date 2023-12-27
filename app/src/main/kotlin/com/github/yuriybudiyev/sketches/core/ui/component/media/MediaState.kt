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

package com.github.yuriybudiyev.sketches.core.ui.component.media

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun rememberMediaState(): MediaState {
    val appContext by rememberUpdatedState(LocalContext.current.applicationContext)
    return remember(appContext) { MediaStateImpl(appContext) }
}

@Stable
interface MediaState {

    val isLoading: Boolean

    val isPlaying: Boolean

    val displayAspectRatio: Float
}

@Stable
private class MediaStateImpl(context: Context): MediaState, Player.Listener, RememberObserver {

    private val player: Player = ExoPlayer
        .Builder(context)
        .build()

    private val isLoadingState: MutableState<Boolean> = mutableStateOf(player.isLoading)
    override val isLoading: Boolean by isLoadingState

    private val isPlayingState: MutableState<Boolean> = mutableStateOf(player.isPlaying)
    override val isPlaying: Boolean by isPlayingState

    private val displayAspectRatioState: MutableFloatState =
        mutableFloatStateOf(calculateDar(player.videoSize))
    override val displayAspectRatio: Float by displayAspectRatioState

    override fun onIsLoadingChanged(isLoading: Boolean) {
        isLoadingState.value = isLoading
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        isPlayingState.value = isPlaying
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        displayAspectRatioState.floatValue = calculateDar(videoSize)
    }

    override fun onAbandoned() {
        if (player.isCommandAvailable(Player.COMMAND_RELEASE)) {
            player.release()
        }
    }

    override fun onForgotten() {
        if (player.isCommandAvailable(Player.COMMAND_RELEASE)) {
            player.release()
        }
    }

    override fun onRemembered() {
    }

    init {
        player.addListener(this)
    }

    private fun calculateDar(videoSize: VideoSize): Float {
        val width = videoSize.width.toFloat()
        val height = videoSize.height.toFloat()
        val ratio = videoSize.pixelWidthHeightRatio
        return if (width > 0f && height > 0f) {
            width * ratio / height
        } else {
            1f
        }
    }
}

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

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource

@Composable
@OptIn(UnstableApi::class)
fun rememberMediaState(): MediaState {
    val appContext by rememberUpdatedState(LocalContext.current.applicationContext)
    return remember(appContext) { MediaStateImpl(appContext) }
}

@Stable
interface MediaState {

    val isLoading: Boolean

    val isPlaying: Boolean

    val displayAspectRatio: Float

    val durationMillis: Long

    val positionMillis: Long

    fun setVideoView(view: SurfaceView)

    fun clearVideoView()

    fun open(
        uri: Uri,
        playWhenReady: Boolean = false
    )

    fun seekTo(positionMillis: Long)

    fun play()

    fun pause()

    fun stop()
}

@Stable
@UnstableApi
private class MediaStateImpl(context: Context): MediaState, Player.Listener, RememberObserver {

    private val player: Player = ExoPlayer
        .Builder(context)
        .setMediaSourceFactory(ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context)))
        .build()

    override var isLoading: Boolean by mutableStateOf(player.isLoading)
        private set

    override var isPlaying: Boolean by mutableStateOf(player.isPlaying)
        private set

    override var displayAspectRatio: Float by mutableFloatStateOf(
        calculateDar(
            player.videoSize,
            player.surfaceSize
        )
    )
        private set

    override var durationMillis: Long by mutableLongStateOf(
        player.callWithCheck(Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            available = { contentDuration },
            unavailable = { 0L })
    )
        private set

    override var positionMillis: Long by mutableLongStateOf(
        player.callWithCheck(Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            available = { contentPosition },
            unavailable = { 0L })
    )
        private set

    override fun setVideoView(view: SurfaceView) {
        callWithCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            player.setVideoSurfaceView(view)
        }
    }

    override fun clearVideoView() {
        if (player.isCommandAvailable(Player.COMMAND_SET_VIDEO_SURFACE)) {
            player.clearVideoSurface()
        }
    }

    override fun open(
        uri: Uri,
        playWhenReady: Boolean
    ) {
        player.callWithCheck(Player.COMMAND_CHANGE_MEDIA_ITEMS) {
            setMediaItem(MediaItem.fromUri(uri))
        }
        player.callWithCheck(Player.COMMAND_PREPARE) {
            prepare()
        }
        player.callWithCheck(Player.COMMAND_PLAY_PAUSE) {
            setPlayWhenReady(playWhenReady)
        }
    }

    override fun seekTo(positionMillis: Long) {
        TODO("Not yet implemented")
    }

    override fun play() {
        if (player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) {
            player.play()
        }
    }

    override fun pause() {
        if (player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) {
            player.pause()
        }
    }

    override fun stop() {
        if (player.isCommandAvailable(Player.COMMAND_STOP)) {
            player.pause()
        }
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        this.isLoading = isLoading
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        this.isPlaying = isPlaying
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        this.displayAspectRatio = calculateDar(
            videoSize,
            player.surfaceSize
        )
    }

    override fun onSurfaceSizeChanged(
        width: Int,
        height: Int
    ) {
        this.displayAspectRatio = calculateDar(
            player.videoSize,
            Size(
                width,
                height
            )
        )
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

    private fun calculateDar(
        videoSize: VideoSize,
        surfaceSize: Size
    ): Float {
        var width = videoSize.width.toFloat()
        var height = videoSize.height.toFloat()
        return if (width > 0f && height > 0f) {
            width * videoSize.pixelWidthHeightRatio / height
        } else {
            width = surfaceSize.width.toFloat()
            height = surfaceSize.height.toFloat()
            if (width > 0f && height > 0f) {
                width / height
            } else {
                1f
            }
        }
    }

    @kotlin.OptIn(ExperimentalContracts::class)
    private inline fun <R, T> R.callWithCheck(
        @Player.Command command: Int,
        available: R.() -> T,
        unavailable: R.() -> T
    ): T {
        contract {
            callsInPlace(
                available,
                InvocationKind.UNKNOWN
            )
            callsInPlace(
                unavailable,
                InvocationKind.UNKNOWN
            )
        }
        return if (player.isCommandAvailable(command)) {
            available()
        } else {
            unavailable()
        }
    }

    @kotlin.OptIn(ExperimentalContracts::class)
    private inline fun <R> R.callWithCheck(
        @Player.Command command: Int,
        available: R.() -> Unit
    ) {
        contract {
            callsInPlace(
                available,
                InvocationKind.UNKNOWN
            )
        }
        if (player.isCommandAvailable(command)) {
            available()
        }
    }
}

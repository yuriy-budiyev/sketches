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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource

@Composable
@OptIn(UnstableApi::class)
fun rememberMediaState(): MediaState {
    val appContext = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    return remember(
        appContext,
        coroutineScope
    ) {
        MediaStateImpl(
            appContext,
            coroutineScope
        )
    }
}

@Stable
interface MediaState {

    val isLoading: Boolean

    val isPlaying: Boolean

    val isVolumeEnabled: Boolean

    val displayAspectRatio: Float

    val isVideoVisible: Boolean

    val durationMillis: Long

    val positionMillis: Long

    fun setVideoView(view: SurfaceView)

    fun clearVideoView()

    fun open(
        uri: Uri,
        playWhenReady: Boolean = false,
        volumeEnabled: Boolean = false
    )

    fun seek(positionMillis: Long)

    fun play()

    fun pause()

    fun stop()

    fun enableVolume()

    fun disableVolume()
}

@Stable
@UnstableApi
private class MediaStateImpl(
    context: Context,
    private val coroutineScope: CoroutineScope
): MediaState, Player.Listener, RememberObserver {

    private val player: Player = ExoPlayer
        .Builder(context)
        .setMediaSourceFactory(ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context)))
        .build()

    override var isLoading: Boolean by mutableStateOf(player.isLoading)
        private set

    override fun onIsLoadingChanged(isLoading: Boolean) {
        this.isLoading = isLoading
    }

    override var isPlaying: Boolean by mutableStateOf(player.isPlaying)
        private set

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        if (isPlaying) {
            updateTimeSpec()
            startTimeSpecPeriodicUpdate()
        } else {
            stopTimeSpecPeriodicUpdate()
            updateTimeSpec()
        }
    }

    private fun isVolumeEnabledInternal(): Boolean =
        player.callWithCheck(Player.COMMAND_GET_VOLUME,
            available = { volume > 0f },
            unavailable = { false })

    override var isVolumeEnabled: Boolean by mutableStateOf(isVolumeEnabledInternal())
        private set

    override fun onVolumeChanged(volume: Float) {
        this.isVolumeEnabled = isVolumeEnabledInternal()
    }

    private fun displayAspectRatioInternal(videoSize: VideoSize = player.videoSize): Float {
        val width = videoSize.width.toFloat()
        val height = videoSize.height.toFloat()
        return if (width > 0f && height > 0f) {
            width * videoSize.pixelWidthHeightRatio / height
        } else {
            1f
        }
    }

    override var displayAspectRatio: Float by mutableFloatStateOf(displayAspectRatioInternal())
        private set

    private fun isVideoVisibleInternal(videoSize: VideoSize = player.videoSize): Boolean =
        videoSize.width > 0 && videoSize.height > 0

    override var isVideoVisible: Boolean by mutableStateOf(isVideoVisibleInternal())
        private set

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        this.displayAspectRatio = displayAspectRatioInternal(videoSize)
        this.isVideoVisible = isVideoVisibleInternal(videoSize)
    }

    override fun setVideoView(view: SurfaceView) {
        player.callWithCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            setVideoSurfaceView(view)
        }
    }

    override fun clearVideoView() {
        player.callWithCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            clearVideoSurface()
        }
    }

    private fun durationMillisInternal(): Long =
        player.callWithCheck(Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            available = { contentDuration },
            unavailable = { 0L })

    override var durationMillis: Long by mutableLongStateOf(durationMillisInternal())
        private set

    private fun positionMillisInternal(): Long =
        player.callWithCheck(Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            available = { contentPosition },
            unavailable = { 0L })

    override var positionMillis: Long by mutableLongStateOf(positionMillisInternal())
        private set

    private fun updateTimeSpec() {
        durationMillis = durationMillisInternal()
        positionMillis = positionMillisInternal()
    }

    private var timeSpecPeriodicUpdateJob: Job? = null

    private fun startTimeSpecPeriodicUpdate() {
        timeSpecPeriodicUpdateJob?.cancel()
        timeSpecPeriodicUpdateJob = coroutineScope.launch {
            while (isActive) {
                delay(250L)
                updateTimeSpec()
            }
        }
    }

    private fun stopTimeSpecPeriodicUpdate() {
        timeSpecPeriodicUpdateJob?.cancel()
        timeSpecPeriodicUpdateJob = null
    }

    override fun open(
        uri: Uri,
        playWhenReady: Boolean,
        volumeEnabled: Boolean
    ) {
        player.callWithCheck(Player.COMMAND_CHANGE_MEDIA_ITEMS) {
            setMediaItem(MediaItem.fromUri(uri))
        }
        player.callWithCheck(Player.COMMAND_PREPARE) {
            prepare()
        }
        if (volumeEnabled) {
            enableVolume()
        } else {
            disableVolume()
        }
        player.callWithCheck(Player.COMMAND_PLAY_PAUSE) {
            setPlayWhenReady(playWhenReady)
        }
    }

    override fun seek(positionMillis: Long) {
        player.callWithCheck(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) {
            seekTo(positionMillis)
        }
    }

    override fun play() {
        player.callWithCheck(Player.COMMAND_PLAY_PAUSE) {
            play()
        }
    }

    override fun pause() {
        player.callWithCheck(Player.COMMAND_PLAY_PAUSE) {
            pause()
        }
    }

    override fun stop() {
        player.callWithCheck(Player.COMMAND_STOP) {
            stop()
        }
    }

    override fun enableVolume() {
        player.callWithCheck(Player.COMMAND_SET_VOLUME) {
            volume = 1f
        }
    }

    override fun disableVolume() {
        player.callWithCheck(Player.COMMAND_SET_VOLUME) {
            volume = 0f
        }
    }

    override fun onAbandoned() {
        stopTimeSpecPeriodicUpdate()
        player.callWithCheck(Player.COMMAND_RELEASE) {
            release()
        }
    }

    override fun onForgotten() {
        stopTimeSpecPeriodicUpdate()
        player.callWithCheck(Player.COMMAND_RELEASE) {
            release()
        }
    }

    override fun onRemembered() {
    }

    init {
        player.addListener(this)
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

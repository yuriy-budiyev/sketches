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
import kotlin.contracts.contract
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.view.TextureView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
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
    return rememberSaveable(
        appContext,
        coroutineScope,
        saver = MediaStateSaver(
            appContext,
            coroutineScope
        )
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

    fun play()

    fun pause()

    fun stop()

    val isVolumeEnabled: Boolean

    fun enableVolume()

    fun disableVolume()

    val isRepeatEnabled: Boolean

    fun enableRepeat()

    fun disableRepeat()

    val isVideoVisible: Boolean

    val displayAspectRatio: Float

    fun setVideoView(view: SurfaceView)

    fun setVideoView(view: TextureView)

    fun clearVideoView()

    val duration: Long

    val position: Long

    fun seek(position: Long)

    val uri: Uri?

    fun open(
        uri: Uri,
        position: Long = 0L,
        playWhenReady: Boolean = false,
        volumeEnabled: Boolean = false,
        repeatEnabled: Boolean = false
    )

    fun close()

    companion object {

        const val TIME_UNKNOWN = C.TIME_UNSET
    }
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
            updatePosition()
            startPositionPeriodicUpdate()
        } else {
            stopPositionPeriodicUpdate()
            updatePosition()
            if (duration != MediaState.TIME_UNKNOWN && position == duration) {
                player.callWithCheck(Player.COMMAND_PLAY_PAUSE) {
                    playWhenReady = false
                }
            }
        }
    }

    override fun play() {
        player.callWithCheck(Player.COMMAND_PLAY_PAUSE) {
            if (this@MediaStateImpl.duration != MediaState.TIME_UNKNOWN && position == duration) {
                seek(0L)
            }
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
            pause()
            seek(0L)
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

    private fun isRepeatEnabledInternal(@Player.RepeatMode repeatMode: Int = player.repeatMode): Boolean =
        repeatMode != Player.REPEAT_MODE_OFF

    override var isRepeatEnabled: Boolean by mutableStateOf(isRepeatEnabledInternal())
        private set

    override fun onRepeatModeChanged(@Player.RepeatMode repeatMode: Int) {
        this.isRepeatEnabled = isRepeatEnabledInternal(repeatMode)
    }

    override fun enableRepeat() {
        player.callWithCheck(Player.COMMAND_SET_REPEAT_MODE) {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    override fun disableRepeat() {
        player.callWithCheck(Player.COMMAND_SET_REPEAT_MODE) {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    private fun isVideoVisibleInternal(videoSize: VideoSize = player.videoSize): Boolean =
        videoSize.width > 0 && videoSize.height > 0

    override var isVideoVisible: Boolean by mutableStateOf(isVideoVisibleInternal())
        private set

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        this.displayAspectRatio = displayAspectRatioInternal(videoSize)
        this.isVideoVisible = isVideoVisibleInternal(videoSize)
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

    override fun setVideoView(view: SurfaceView) {
        player.callWithCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            setVideoSurfaceView(view)
        }
    }

    override fun setVideoView(view: TextureView) {
        player.callWithCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            setVideoTextureView(view)
        }
    }

    override fun clearVideoView() {
        player.callWithCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            clearVideoSurface()
        }
    }

    private fun durationInternal(): Long =
        player.callWithCheck(Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            available = { contentDuration },
            unavailable = { MediaState.TIME_UNKNOWN })

    override var duration: Long by mutableLongStateOf(durationInternal())
        private set

    override fun onTimelineChanged(
        timeline: Timeline,
        @Player.TimelineChangeReason reason: Int
    ) {
    }

    private fun positionInternal(): Long =
        player.callWithCheck(Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            available = { contentPosition },
            unavailable = { MediaState.TIME_UNKNOWN })

    override var position: Long by mutableLongStateOf(positionInternal())
        private set

    private fun updatePosition() {
        this.position = positionInternal()
    }

    override fun seek(position: Long) {
        player.callWithCheck(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) {
            if (isPlaying) {
                stopPositionPeriodicUpdate()
            }
            seekTo(position)
            updatePosition()
            if (isPlaying) {
                startPositionPeriodicUpdate()
            }
        }
    }

    private var positionPeriodicUpdateJob: Job? = null

    private fun startPositionPeriodicUpdate() {
        positionPeriodicUpdateJob?.cancel()
        positionPeriodicUpdateJob = coroutineScope.launch {
            while (isActive) {
                delay(250L)
                updatePosition()
            }
        }
    }

    private fun stopPositionPeriodicUpdate() {
        positionPeriodicUpdateJob?.cancel()
        positionPeriodicUpdateJob = null
    }

    override var uri: Uri? by mutableStateOf(null)
        private set

    override fun open(
        uri: Uri,
        position: Long,
        playWhenReady: Boolean,
        volumeEnabled: Boolean,
        repeatEnabled: Boolean
    ) {
        player.callWithCheck(Player.COMMAND_CHANGE_MEDIA_ITEMS) {
            setMediaItem(
                MediaItem.fromUri(uri),
                position
            )
            this@MediaStateImpl.uri = uri
        }
        player.callWithCheck(Player.COMMAND_PREPARE) {
            prepare()
        }
        if (volumeEnabled) {
            enableVolume()
        } else {
            disableVolume()
        }
        if (repeatEnabled) {
            enableRepeat()
        } else {
            disableRepeat()
        }
        player.callWithCheck(Player.COMMAND_PLAY_PAUSE) {
            setPlayWhenReady(playWhenReady)
        }
    }

    override fun close() {
        uri = null
        player.callWithCheck(Player.COMMAND_STOP) {
            player.stop()
        }
        player.callWithCheck(Player.COMMAND_CHANGE_MEDIA_ITEMS) {
            player.clearMediaItems()
        }
    }

    override fun onAbandoned() {
        stopPositionPeriodicUpdate()
        player.callWithCheck(Player.COMMAND_RELEASE) {
            release()
            uri = null
        }
    }

    override fun onForgotten() {
        stopPositionPeriodicUpdate()
        player.callWithCheck(Player.COMMAND_RELEASE) {
            release()
            uri = null
        }
    }

    override fun onRemembered() {
    }

    init {
        player.addListener(this)
    }

    @kotlin.OptIn(ExperimentalContracts::class)
    private inline fun Player.callWithCheck(
        @Player.Command command: Int,
        crossinline available: Player.() -> Unit
    ) {
        contract {
            callsInPlace(available)
        }
        if (isCommandAvailable(command)) {
            available()
        }
    }

    @kotlin.OptIn(ExperimentalContracts::class)
    private inline fun <T> Player.callWithCheck(
        @Player.Command command: Int,
        available: Player.() -> T,
        unavailable: Player.() -> T
    ): T {
        contract {
            callsInPlace(available)
            callsInPlace(unavailable)
        }
        return if (isCommandAvailable(command)) {
            available()
        } else {
            unavailable()
        }
    }
}

@UnstableApi
private class MediaStateSaver(
    private val context: Context,
    private val coroutineScope: CoroutineScope
): Saver<MediaStateImpl, Bundle> {

    @Suppress("DEPRECATION")
    override fun restore(value: Bundle): MediaStateImpl =
        MediaStateImpl(
            context,
            coroutineScope
        ).apply {
            val isVolumeEnabled = value.getBoolean(
                IS_VOLUME_ENABLED,
                false
            )
            val isRepeatEnabled = value.getBoolean(
                IS_REPEAT_ENABLED,
                false
            )
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                value.getParcelable(
                    URI,
                    Uri::class.java
                )
            } else {
                value.getParcelable(URI)
            }
            if (uri != null) {
                val isPlaying = value.getBoolean(
                    IS_PLAYING,
                    false
                )
                val position = value.getLong(
                    POSITION,
                    0L
                )
                open(
                    uri = uri,
                    position = position,
                    playWhenReady = isPlaying,
                    volumeEnabled = isVolumeEnabled,
                    repeatEnabled = isRepeatEnabled
                )
            } else {
                if (isVolumeEnabled) {
                    enableVolume()
                } else {
                    disableVolume()
                }
                if (isRepeatEnabled) {
                    enableRepeat()
                } else {
                    disableRepeat()
                }
            }
        }

    override fun SaverScope.save(value: MediaStateImpl): Bundle =
        Bundle().apply {
            putBoolean(
                IS_PLAYING,
                value.isPlaying
            )
            putBoolean(
                IS_VOLUME_ENABLED,
                value.isVolumeEnabled
            )
            putBoolean(
                IS_REPEAT_ENABLED,
                value.isRepeatEnabled
            )
            putLong(
                POSITION,
                value.position
            )
            putParcelable(
                URI,
                value.uri
            )
        }

    companion object {

        const val IS_PLAYING = "is_playing"
        const val IS_VOLUME_ENABLED = "is_volume_enabled"
        const val IS_REPEAT_ENABLED = "is_repeat_enabled"
        const val POSITION = "position"
        const val URI = "uri"
    }
}

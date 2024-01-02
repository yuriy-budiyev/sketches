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
import androidx.annotation.FloatRange
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
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

    @get:FloatRange(
        from = 0.0,
        to = 1.0
    )
    val position: Float

    fun seek(
        @FloatRange(
            from = 0.0,
            to = 1.0
        ) position: Float
    )

    val uri: Uri?

    fun open(
        uri: Uri,
        @FloatRange(
            from = 0.0,
            to = 1.0
        ) position: Float = 0f,
        playWhenReady: Boolean = false,
        volumeEnabled: Boolean = false,
        repeatEnabled: Boolean = false
    )
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
            if (position == 1f) {
                player.callWithCheckOrEnqueue(Player.COMMAND_PLAY_PAUSE) {
                    playWhenReady = false
                }
            }
        }
    }

    override fun play() {
        player.callWithCheckOrEnqueue(Player.COMMAND_PLAY_PAUSE) {
            if (position == 1f) {
                seek(0f)
            }
            play()
        }
    }

    override fun pause() {
        player.callWithCheckOrEnqueue(Player.COMMAND_PLAY_PAUSE) {
            pause()
        }
    }

    override fun stop() {
        player.callWithCheckOrEnqueue(Player.COMMAND_STOP) {
            pause()
            seek(0f)
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
        player.callWithCheckOrEnqueue(Player.COMMAND_SET_VOLUME) {
            volume = 1f
        }
    }

    override fun disableVolume() {
        player.callWithCheckOrEnqueue(Player.COMMAND_SET_VOLUME) {
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
        player.callWithCheckOrEnqueue(Player.COMMAND_SET_REPEAT_MODE) {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    override fun disableRepeat() {
        player.callWithCheckOrEnqueue(Player.COMMAND_SET_REPEAT_MODE) {
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
        player.callWithCheckOrEnqueue(Player.COMMAND_SET_VIDEO_SURFACE) {
            setVideoSurfaceView(view)
        }
    }

    override fun setVideoView(view: TextureView) {
        player.callWithCheckOrEnqueue(Player.COMMAND_SET_VIDEO_SURFACE) {
            setVideoTextureView(view)
        }
    }

    override fun clearVideoView() {
        player.callWithCheckOrEnqueue(Player.COMMAND_SET_VIDEO_SURFACE) {
            clearVideoSurface()
        }
    }

    @FloatRange(
        from = 0.0,
        to = 1.0
    )
    private fun positionInternal(): Float =
        player
            .callWithCheck(Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                available = { contentPosition.toFloat() / contentDuration.toFloat() },
                unavailable = { 0f })
            .coerceIn(
                minimumValue = 0f,
                maximumValue = 1f
            )

    @get:FloatRange(
        from = 0.0,
        to = 1.0
    )
    @setparam:FloatRange(
        from = 0.0,
        to = 1.0
    )
    override var position: Float by mutableFloatStateOf(positionInternal())
        private set

    private fun updatePosition() {
        this.position = positionInternal()
    }

    override fun seek(
        @FloatRange(
            from = 0.0,
            to = 1.0
        ) position: Float
    ) {
        check(position in 0f..1f) { "Position should be in range of 0..1" }
        player.callWithCheckOrEnqueue(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) {
            val duration = callWithCheck(Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                available = { contentDuration },
                unavailable = { 0L })
            if (isPlaying) {
                stopPositionPeriodicUpdate()
            }
            seekTo((duration * position).toLong())
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
        @FloatRange(
            from = 0.0,
            to = 1.0
        ) position: Float,
        playWhenReady: Boolean,
        volumeEnabled: Boolean,
        repeatEnabled: Boolean
    ) {
        pendingCommands.clear()
        player.callWithCheckOrEnqueue(Player.COMMAND_CHANGE_MEDIA_ITEMS) {
            setMediaItem(MediaItem.fromUri(uri))
            this@MediaStateImpl.uri = uri
        }
        player.callWithCheckOrEnqueue(Player.COMMAND_PREPARE) {
            prepare()
        }
        if (position != 0f) {
            seek(position)
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
        player.callWithCheckOrEnqueue(Player.COMMAND_PLAY_PAUSE) {
            setPlayWhenReady(playWhenReady)
        }
    }

    override fun onAbandoned() {
        pendingCommands.clear()
        stopPositionPeriodicUpdate()
        player.callWithCheckOrEnqueue(Player.COMMAND_RELEASE) {
            release()
            uri = null
        }
    }

    override fun onForgotten() {
        pendingCommands.clear()
        stopPositionPeriodicUpdate()
        player.callWithCheckOrEnqueue(Player.COMMAND_RELEASE) {
            release()
            uri = null
        }
    }

    override fun onRemembered() {
    }

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        val iterator = pendingCommands.iterator()
        while (iterator.hasNext()) {
            val command = iterator.next()
            if (command.command in availableCommands) {
                iterator.remove()
                command.call()
            }
        }
    }

    private val pendingCommands: MutableSet<PlayerCommand> = LinkedHashSet()

    @kotlin.OptIn(ExperimentalContracts::class)
    private inline fun Player.callWithCheckOrEnqueue(
        @Player.Command command: Int,
        crossinline available: Player.() -> Unit
    ) {
        contract {
            callsInPlace(available)
        }
        if (isCommandAvailable(command)) {
            available()
        } else {
            pendingCommands += object: PlayerCommand(command) {

                override fun call() {
                    available()
                }
            }
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

    init {
        player.addListener(this)
    }

    private abstract inner class PlayerCommand(@Player.Command val command: Int) {

        abstract fun call()

        final override fun equals(other: Any?): Boolean =
            when {
                other === this -> true
                other is PlayerCommand -> other.command == this.command
                else -> false
            }

        final override fun hashCode(): Int =
            command
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
                val position = value.getFloat(
                    POSITION,
                    0f
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
            putFloat(
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

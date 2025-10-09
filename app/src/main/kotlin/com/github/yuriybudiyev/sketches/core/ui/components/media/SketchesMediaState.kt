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

package com.github.yuriybudiyev.sketches.core.ui.components.media

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.FloatRange
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Composable
@OptIn(UnstableApi::class)
fun rememberSketchesMediaState(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): SketchesMediaState {
    val appContext = LocalContext.current.applicationContext
    return rememberSaveable(
        appContext,
        coroutineScope,
        saver = SketchesMediaStateImplSaver(
            appContext,
            coroutineScope
        )
    ) {
        SketchesMediaStateImpl(
            appContext,
            coroutineScope
        )
    }
}

@Stable
sealed interface SketchesMediaState {

    val coroutineScope: CoroutineScope

    val isLoading: Boolean

    val isPlaying: Boolean

    val isPlaybackError: Boolean

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

    @get:FloatRange(
        from = 0.0,
        fromInclusive = false
    )
    val displayAspectRatio: Float

    fun setVideoView(view: SurfaceView)

    fun clearVideoView(view: SurfaceView)

    fun setVideoView(view: TextureView)

    fun clearVideoView(view: TextureView)

    fun setVideoSurface(surface: Surface)

    fun clearVideoSurface(surface: Surface)

    fun clearVideoSurfaceOrView()

    val duration: Long

    val position: Long

    fun seek(position: Long)

    val uri: String?

    fun open(
        uri: String,
        position: Long = 0L,
        playWhenReady: Boolean = false,
        volumeEnabled: Boolean = false,
        repeatEnabled: Boolean = false,
    )

    fun close()

    companion object {

        const val UnknownTime: Long = Long.MIN_VALUE
    }
}

@Stable
@UnstableApi
private class SketchesMediaStateImpl @RememberInComposition constructor(
    context: Context,
    override val coroutineScope: CoroutineScope,
): SketchesMediaState, Player.Listener, RememberObserver {

    private val player: Player = ExoPlayer
        .Builder(context)
        .setLoadControl(
            DefaultLoadControl
                .Builder()
                .setTargetBufferBytes(context.calculateBufferSize())
                .build()
        )
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
            if (checkEndOfContent()) {
                player.withCheck(Player.COMMAND_PLAY_PAUSE) {
                    playWhenReady = false
                }
            }
        }
    }

    override var isPlaybackError: Boolean by mutableStateOf(player.playerError != null)
        private set

    override fun onPlayerErrorChanged(error: PlaybackException?) {
        isPlaybackError = error != null
    }

    override fun play() {
        player.withCheck(Player.COMMAND_PLAY_PAUSE) {
            if (checkEndOfContent()) {
                seek(0L)
            }
            play()
        }
    }

    override fun pause() {
        player.withCheck(Player.COMMAND_PLAY_PAUSE) {
            pause()
        }
    }

    override fun stop() {
        player.withCheck(Player.COMMAND_STOP) {
            pause()
            seek(0L)
        }
    }

    private fun isVolumeEnabledInternal(): Boolean =
        player.withCheck(
            Player.COMMAND_GET_VOLUME,
            available = { volume > 0f },
            unavailable = { false },
        )

    override var isVolumeEnabled: Boolean by mutableStateOf(isVolumeEnabledInternal())
        private set

    override fun onVolumeChanged(volume: Float) {
        this.isVolumeEnabled = isVolumeEnabledInternal()
    }

    override fun enableVolume() {
        player.withCheck(Player.COMMAND_SET_VOLUME) {
            volume = 1f
        }
    }

    override fun disableVolume() {
        player.withCheck(Player.COMMAND_SET_VOLUME) {
            volume = 0f
        }
    }

    private fun isRepeatEnabledInternal(
        @Player.RepeatMode
        repeatMode: Int = player.repeatMode,
    ): Boolean =
        repeatMode != Player.REPEAT_MODE_OFF

    override var isRepeatEnabled: Boolean by mutableStateOf(isRepeatEnabledInternal())
        private set

    override fun onRepeatModeChanged(
        @Player.RepeatMode
        repeatMode: Int,
    ) {
        this.isRepeatEnabled = isRepeatEnabledInternal(repeatMode)
    }

    override fun enableRepeat() {
        player.withCheck(Player.COMMAND_SET_REPEAT_MODE) {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    override fun disableRepeat() {
        player.withCheck(Player.COMMAND_SET_REPEAT_MODE) {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    private fun isVideoVisibleInternal(videoSize: VideoSize = player.videoSize): Boolean =
        videoSize.width > 0 && videoSize.height > 0

    override var isVideoVisible: Boolean by mutableStateOf(isVideoVisibleInternal())
        private set

    @FloatRange(
        from = 0.0,
        fromInclusive = false
    )
    private fun displayAspectRatioInternal(videoSize: VideoSize = player.videoSize): Float {
        val width = videoSize.width.toFloat()
        val height = videoSize.height.toFloat()
        return if (width > 0f && height > 0f) {
            width * videoSize.pixelWidthHeightRatio / height
        } else {
            1f
        }
    }

    @get:FloatRange(
        from = 0.0,
        fromInclusive = false
    )
    @setparam:FloatRange(
        from = 0.0,
        fromInclusive = false
    )
    override var displayAspectRatio: Float by mutableFloatStateOf(displayAspectRatioInternal())
        private set

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        this.displayAspectRatio = displayAspectRatioInternal(videoSize)
        this.isVideoVisible = isVideoVisibleInternal(videoSize)
    }

    override fun setVideoView(view: SurfaceView) {
        player.withCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            setVideoSurfaceView(view)
        }
    }

    override fun clearVideoView(view: SurfaceView) {
        player.withCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            clearVideoSurfaceView(view)
        }
    }

    override fun setVideoView(view: TextureView) {
        player.withCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            setVideoTextureView(view)
        }
    }

    override fun clearVideoView(view: TextureView) {
        player.withCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            clearVideoTextureView(view)
        }
    }

    override fun setVideoSurface(surface: Surface) {
        player.withCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            setVideoSurface(surface)
        }
    }

    override fun clearVideoSurface(surface: Surface) {
        player.withCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            clearVideoSurface(surface)
        }
    }

    override fun clearVideoSurfaceOrView() {
        player.withCheck(Player.COMMAND_SET_VIDEO_SURFACE) {
            clearVideoSurface()
        }
    }

    private fun durationInternal(): Long =
        player.withCheck(
            Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            available = {
                val contentDuration = contentDuration
                if (contentDuration != C.TIME_UNSET) {
                    contentDuration.coerceAtLeast(0L)
                } else {
                    SketchesMediaState.UnknownTime
                }
            },
            unavailable = { SketchesMediaState.UnknownTime },
        )

    override var duration: Long by mutableLongStateOf(durationInternal())
        private set

    private fun updateDuration(duration: Long = durationInternal()) {
        this.duration = duration
    }

    private fun positionInternal(): Long =
        player.withCheck(
            Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            available = {
                correctPosition(
                    position = contentPosition,
                    duration = contentDuration,
                    unknownToCheck = C.TIME_UNSET,
                    unknownToReturn = SketchesMediaState.UnknownTime
                )
            },
            unavailable = { SketchesMediaState.UnknownTime },
        )

    override var position: Long by mutableLongStateOf(positionInternal())
        private set

    private fun updatePosition(position: Long = positionInternal()) {
        this.position = position
    }

    private fun correctPosition(
        position: Long = this.position,
        duration: Long = this.duration,
        unknownToCheck: Long = SketchesMediaState.UnknownTime,
        unknownToReturn: Long = SketchesMediaState.UnknownTime,
    ): Long =
        when {
            position != unknownToCheck && duration != unknownToCheck -> {
                position.coerceIn(
                    minimumValue = 0L,
                    maximumValue = duration
                )
            }
            position != unknownToCheck -> {
                position
            }
            else -> {
                unknownToReturn
            }
        }

    override fun onTimelineChanged(
        timeline: Timeline,
        @Player.TimelineChangeReason
        reason: Int,
    ) {
        updateDuration()
        updatePosition()
    }

    private fun checkEndOfContent(
        position: Long = this.position,
        duration: Long = this.duration,
    ): Boolean =
        if (
            position != SketchesMediaState.UnknownTime
            && duration != SketchesMediaState.UnknownTime
        ) {
            position == duration
        } else {
            false
        }

    override fun seek(position: Long) {
        player.withCheck(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) {
            stopPositionPeriodicUpdate()
            val correctedPosition = correctPosition(position)
            if (correctedPosition != SketchesMediaState.UnknownTime) {
                seekTo(correctedPosition)
                updatePosition(correctedPosition)
            }
            if (isPlaying) {
                startPositionPeriodicUpdate()
            }
        }
    }

    private var positionPeriodicUpdateJob: Job? = null

    private fun startPositionPeriodicUpdate() {
        positionPeriodicUpdateJob?.cancel()
        positionPeriodicUpdateJob = coroutineScope.launch {
            try {
                while (isActive) {
                    withFrameNanos {
                        updatePosition()
                    }
                }
            } catch (_: CancellationException) {
            }
        }
    }

    private fun stopPositionPeriodicUpdate() {
        positionPeriodicUpdateJob?.cancel()
        positionPeriodicUpdateJob = null
    }

    override var uri: String? by mutableStateOf(null)
        private set

    override fun open(
        uri: String,
        position: Long,
        playWhenReady: Boolean,
        volumeEnabled: Boolean,
        repeatEnabled: Boolean,
    ) {
        player.withCheck(Player.COMMAND_CHANGE_MEDIA_ITEMS) {
            setMediaItem(
                MediaItem.fromUri(uri),
                position
            )
            this@SketchesMediaStateImpl.uri = uri
        }
        player.withCheck(Player.COMMAND_PREPARE) {
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
        player.withCheck(Player.COMMAND_PLAY_PAUSE) {
            setPlayWhenReady(playWhenReady)
        }
    }

    private fun releasePlayer() {
        uri = null
        stopPositionPeriodicUpdate()
        player.withCheck(Player.COMMAND_RELEASE) {
            release()
        }
    }

    override fun close() {
        player.withCheck(Player.COMMAND_STOP) {
            player.stop()
        }
        player.withCheck(Player.COMMAND_CHANGE_MEDIA_ITEMS) {
            player.clearMediaItems()
        }
        releasePlayer()
    }

    override fun onRemembered() {
    }

    override fun onForgotten() {
        releasePlayer()
    }

    override fun onAbandoned() {
        releasePlayer()
    }

    init {
        player.addListener(this)
    }
}

@UnstableApi
private class SketchesMediaStateImplSaver(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
): Saver<SketchesMediaStateImpl, Bundle> {

    override fun restore(value: Bundle): SketchesMediaStateImpl =
        SketchesMediaStateImpl(
            context,
            coroutineScope
        ).apply {
            val volumeEnabled = value.getBoolean(
                Keys.VolumeEnabled,
                false
            )
            val repeatEnabled = value.getBoolean(
                Keys.RepeatEnabled,
                false
            )
            val uri = value.getString(Keys.Uri)
            if (uri != null) {
                val playing = value.getBoolean(
                    Keys.Playing,
                    false
                )
                val position = value.getLong(
                    Keys.Position,
                    0L
                )
                open(
                    uri = uri,
                    position = position,
                    playWhenReady = playing,
                    volumeEnabled = volumeEnabled,
                    repeatEnabled = repeatEnabled
                )
            } else {
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
            }
        }

    override fun SaverScope.save(value: SketchesMediaStateImpl): Bundle =
        Bundle().apply {
            putBoolean(
                Keys.Playing,
                value.isPlaying
            )
            putBoolean(
                Keys.VolumeEnabled,
                value.isVolumeEnabled
            )
            putBoolean(
                Keys.RepeatEnabled,
                value.isRepeatEnabled
            )
            putLong(
                Keys.Position,
                value.position
            )
            putString(
                Keys.Uri,
                value.uri
            )
        }

    private object Keys {

        const val Playing: String = "playing"
        const val VolumeEnabled: String = "volume_enabled"
        const val RepeatEnabled: String = "repeat_enabled"
        const val Position: String = "position"
        const val Uri: String = "uri"
    }
}

@kotlin.OptIn(ExperimentalContracts::class)
private inline fun Player.withCheck(
    @Player.Command
    command: Int,
    available: Player.() -> Unit,
) {
    contract {
        callsInPlace(
            available,
            InvocationKind.AT_MOST_ONCE
        )
    }
    if (isCommandAvailable(command)) {
        available()
    }
}

@kotlin.OptIn(ExperimentalContracts::class)
private inline fun <T> Player.withCheck(
    @Player.Command
    command: Int,
    available: Player.() -> T,
    unavailable: Player.() -> T,
): T {
    contract {
        callsInPlace(
            available,
            InvocationKind.AT_MOST_ONCE
        )
        callsInPlace(
            unavailable,
            InvocationKind.AT_MOST_ONCE
        )
    }
    return if (isCommandAvailable(command)) {
        available()
    } else {
        unavailable()
    }
}

private fun Context.calculateBufferSize(): Int {
    val activityManager = getSystemService(ActivityManager::class.java)
    val largeHeap = (applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
    val memoryClass =
        if (largeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
    return memoryClass * 1024 * 1024 / 8
}

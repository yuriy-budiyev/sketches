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

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun rememberMediaPlayerState(context: Context): MediaPlayerState {
    val appContext by rememberUpdatedState(context.applicationContext)
    return remember(appContext) { MediaPlayerState(appContext) }
}

@Stable
class MediaPlayerState(context: Context): RememberObserver {

    val isLoadingState: State<Boolean>
        get() = isLoadingStateInternal

    val isPlayingState: State<Boolean>
        get() = isPlayingStateInternal

    val videoSizeState: State<VideoSize>
        get() = videoSizeStateInternal

    val videoDisplayAspectRatioState: FloatState
        get() = videoDisplayAspectRatioStateInternal

    fun setSurfaceView(view: SurfaceView) {
        player.clearVideoSurface()
        player.setVideoSurfaceView(view)
    }

    fun open(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    fun play() {
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun dispose() {
        player.clearVideoSurface()
        player.release()
    }

    override fun onAbandoned() {
        dispose()
    }

    override fun onForgotten() {
        dispose()
    }

    override fun onRemembered() {
    }

    private val player: ExoPlayer
    private val isLoadingStateInternal: MutableState<Boolean>
    private val isPlayingStateInternal: MutableState<Boolean>
    private val videoSizeStateInternal: MutableState<VideoSize>
    private val videoDisplayAspectRatioStateInternal: MutableFloatState

    init {
        player = ExoPlayer
            .Builder(context)
            .build()
        isLoadingStateInternal = mutableStateOf(player.isLoading)
        isPlayingStateInternal = mutableStateOf(player.isPlaying)
        videoSizeStateInternal = mutableStateOf(player.videoSize)
        videoDisplayAspectRatioStateInternal = mutableFloatStateOf(1f)
        player.addListener(PlayerListener())
    }

    private inner class PlayerListener: Player.Listener {

        override fun onEvents(
            player: Player,
            events: Player.Events
        ) {
        }

        override fun onTimelineChanged(
            timeline: Timeline,
            reason: Int
        ) {
        }

        override fun onMediaItemTransition(
            mediaItem: MediaItem?,
            reason: Int
        ) {
        }

        override fun onTracksChanged(tracks: Tracks) {
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        }

        override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            isLoadingStateInternal.value = isLoading
        }

        override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
        }

        override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
        }

        override fun onPlayWhenReadyChanged(
            playWhenReady: Boolean,
            reason: Int
        ) {
        }

        override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            isPlayingStateInternal.value = isPlaying
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        }

        override fun onPlayerError(error: PlaybackException) {
        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        }

        override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
        }

        override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
        }

        override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) {
        }

        override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {
        }

        override fun onVolumeChanged(volume: Float) {
        }

        override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
        }

        override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
        }

        override fun onDeviceVolumeChanged(
            volume: Int,
            muted: Boolean
        ) {
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            videoSizeStateInternal.value = videoSize

            val width = videoSize.width
            val height = videoSize.height
            videoDisplayAspectRatioStateInternal.floatValue = if (width > 0 && height > 0) {
                width * videoSize.pixelWidthHeightRatio / height
            } else {
                1f
            }
        }

        override fun onSurfaceSizeChanged(
            width: Int,
            height: Int
        ) {
        }

        override fun onRenderedFirstFrame() {
        }

        override fun onCues(cueGroup: CueGroup) {
        }
    }
}

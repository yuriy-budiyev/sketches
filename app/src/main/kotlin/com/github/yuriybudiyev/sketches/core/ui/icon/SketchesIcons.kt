package com.github.yuriybudiyev.sketches.core.ui.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Image

object SketchesIcons {

    val ImagesSelected = Icons.Filled.Photo
    val ImagesUnselected = Icons.Outlined.Photo
    val BucketsSelected = Icons.Filled.PhotoLibrary
    val BucketsUnselected = Icons.Outlined.PhotoLibrary
    val ImageLoading = Icons.Rounded.Image
    val ImageError = Icons.Rounded.BrokenImage
    val Share = Icons.Filled.Share
    val Delete = Icons.Filled.DeleteForever
    val Video = Icons.Outlined.PlayCircle
    val Play = Icons.Filled.PlayArrow
    val Pause = Icons.Filled.Pause
    val VolumeEnabled = Icons.AutoMirrored.Filled.VolumeUp
    val VolumeDisabled = Icons.AutoMirrored.Filled.VolumeOff
}

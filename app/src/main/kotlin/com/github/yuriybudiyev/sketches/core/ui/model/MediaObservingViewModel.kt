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

package com.github.yuriybudiyev.sketches.core.ui.model

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.MediaAccess
import com.github.yuriybudiyev.sketches.core.platform.permissions.media.checkMediaAccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.measureTime

abstract class MediaObservingViewModel(context: Context): ViewModel() {

    @MainThread
    protected abstract fun onMediaChanged()

    fun updateMediaAccess() {
        val current = mediaAccess
        val updated = appContext.checkMediaAccess()
        mediaAccess = updated
        if (current != updated || updated == MediaAccess.UserSelected) {
            viewModelScope.launch {
                onMediaChanged()
            }
        }
    }

    @CallSuper
    override fun onCleared() {
        with(appContext.contentResolver) {
            unregisterContentObserver(imagesObserver)
            unregisterContentObserver(videoObserver)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private val appContext: Context = context.applicationContext

    private var mediaAccess: MediaAccess = appContext.checkMediaAccess()

    private val imagesObserver: ContentObserver = Observer()
    private val videoObserver: ContentObserver = Observer()

    init {
        with(appContext.contentResolver) {
            registerContentObserver(
                MediaType.Image.contentUri,
                true,
                imagesObserver
            )
            registerContentObserver(
                MediaType.Video.contentUri,
                true,
                videoObserver
            )
        }
    }

    private inner class Observer: ContentObserver(Handler(Looper.getMainLooper())) {

        private var mediaChangedJob: Job? = null
        private var delayResetJob: Job? = null
        private var currentDelay: Long = 0L

        override fun onChange(selfChange: Boolean) {
            mediaChangedJob?.cancel()
            mediaChangedJob = viewModelScope.launch {
                val delayed = measureTime {
                    try {
                        delay(timeMillis = currentDelay)
                    } catch (_: CancellationException) {
                    }
                }
                currentDelay -= delayed.toLong(DurationUnit.MILLISECONDS)
                if (currentDelay <= 0L) {
                    currentDelay = 1000L
                }
                if (isActive) {
                    onMediaChanged()
                }
            }
            delayResetJob?.cancel()
            delayResetJob = viewModelScope.launch {
                delay(timeMillis = currentDelay + 250L)
                currentDelay = 0L
            }
        }
    }
}

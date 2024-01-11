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

package com.github.yuriybudiyev.sketches.feature.image.ui

import android.annotation.SuppressLint
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.data.model.MediaType
import com.github.yuriybudiyev.sketches.core.data.repository.MediaStoreRepository
import com.github.yuriybudiyev.sketches.core.util.coroutines.excludeCancellation
import com.github.yuriybudiyev.sketches.core.util.data.contentUriFor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class ImageScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MediaStoreRepository,
): ViewModel() {

    private val uiStateInternal: MutableStateFlow<ImageScreenUiState> =
        MutableStateFlow(ImageScreenUiState.Loading)

    val uiState: StateFlow<ImageScreenUiState>
        get() = uiStateInternal

    fun updateMedia(
        fileIndex: Int,
        fileId: Long,
        bucketId: Long,
        silent: Boolean = uiState.value is ImageScreenUiState.Image,
    ) {
        setCurrentMediaData(
            fileIndex,
            fileId,
            bucketId
        )
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            if (!silent) {
                uiStateInternal.value = ImageScreenUiState.Loading
            }
            try {
                val files = withContext(Dispatchers.Default) { repository.getFiles(bucketId) }
                if (files.isNotEmpty()) {
                    if (files[fileIndex].id == fileId) {
                        uiStateInternal.value = ImageScreenUiState.Image(
                            fileIndex,
                            fileId,
                            bucketId,
                            files
                        )
                    } else {
                        var backwardIndex = fileIndex - 1
                        var forwardIndex = fileIndex + 1
                        var actualIndex = fileIndex
                        val imagesSize = files.size
                        val startBound = (fileIndex - 8).coerceAtLeast(-1)
                        val endBound = (fileIndex + 8).coerceAtMost(imagesSize)
                        while (backwardIndex > startBound || forwardIndex < endBound) {
                            if (backwardIndex > -1) {
                                if (files[backwardIndex].id == fileId) {
                                    actualIndex = backwardIndex
                                    break
                                }
                                backwardIndex--
                            }
                            if (forwardIndex < imagesSize) {
                                if (files[forwardIndex].id == fileId) {
                                    actualIndex = forwardIndex
                                    break
                                }
                                forwardIndex++
                            }
                        }
                        uiStateInternal.value = ImageScreenUiState.Image(
                            actualIndex.coerceIn(
                                0,
                                imagesSize - 1
                            ),
                            fileId,
                            bucketId,
                            files
                        )
                    }
                } else {
                    if (!silent) {
                        uiStateInternal.value = ImageScreenUiState.Empty
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    excludeCancellation(e) {
                        uiStateInternal.value = ImageScreenUiState.Error(e)
                    }
                }
            }
        }
    }

    fun setCurrentMediaData(
        fileIndex: Int,
        fileId: Long,
        bucketId: Long,
    ) {
        currentFileIndex = fileIndex
        currentFileId = fileId
        currentBucketId = bucketId
    }

    override fun onCleared() {
        with(context.contentResolver) {
            unregisterContentObserver(imagesObserver)
            unregisterContentObserver(videoObserver)
        }
    }

    private var currentJob: Job? = null
    private var currentFileIndex: Int = -1
    private var currentFileId: Long = -1L
    private var currentBucketId: Long = -1L

    private val imagesObserver: ContentObserver =
        object: ContentObserver(Handler(Looper.getMainLooper())) {

            override fun onChange(selfChange: Boolean) {
                updateMediaInternal()
            }
        }

    private val videoObserver: ContentObserver =
        object: ContentObserver(Handler(Looper.getMainLooper())) {

            override fun onChange(selfChange: Boolean) {
                updateMediaInternal()
            }
        }

    init {
        with(context.contentResolver) {
            registerContentObserver(
                contentUriFor(MediaType.IMAGE),
                true,
                imagesObserver
            )
            registerContentObserver(
                contentUriFor(MediaType.VIDEO),
                true,
                imagesObserver
            )
        }
    }

    private fun updateMediaInternal() {
        val fileIndex = currentFileIndex
        val fileId = currentFileId
        val bucketId = currentBucketId
        if (fileIndex != -1) {
            updateMedia(
                fileIndex,
                fileId,
                bucketId
            )
        }
    }
}

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

package com.github.yuriybudiyev.sketches.feature.image.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.common.coroutines.excludeCancellation
import com.github.yuriybudiyev.sketches.core.data.repository.MediaStoreRepository
import com.github.yuriybudiyev.sketches.core.multithreading.Worker
import com.github.yuriybudiyev.sketches.core.ui.model.MediaObservingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImageScreenViewModel @Inject constructor(
    @ApplicationContext
    context: Context,
    private val repository: MediaStoreRepository,
): MediaObservingViewModel(context) {

    private val uiStateInternal: MutableStateFlow<ImageScreenUiState> =
        MutableStateFlow(ImageScreenUiState.Loading)

    val uiState: StateFlow<ImageScreenUiState>
        get() = uiStateInternal

    fun setCurrentMediaData(
        fileIndex: Int,
        fileId: Long,
        bucketId: Long,
    ) {
        currentFileIndex = fileIndex
        currentFileId = fileId
        currentBucketId = bucketId
    }

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
                val files = withContext(Dispatchers.Worker) { repository.getFiles(bucketId) }
                val filesSize = files.size
                if (filesSize > 0) {
                    if (fileIndex < filesSize && files[fileIndex].id == fileId) {
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
                        while (backwardIndex > -1 || forwardIndex < filesSize) {
                            if (backwardIndex > -1) {
                                if (files[backwardIndex].id == fileId) {
                                    actualIndex = backwardIndex
                                    break
                                }
                                backwardIndex--
                            }
                            if (forwardIndex < filesSize) {
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
                                filesSize - 1
                            ),
                            fileId,
                            bucketId,
                            files
                        )
                    }
                } else {
                    uiStateInternal.value = ImageScreenUiState.Empty
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

    fun deleteMedia(uri: Uri) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.Worker) {
                    repository.deleteFile(uri)
                }
            } catch (e: Exception) {
                excludeCancellation(e) {
                    uiStateInternal.value = ImageScreenUiState.Error(e)
                }
            }
        }
    }

    override fun onMediaChanged() {
        updateMediaInternal()
    }

    private var currentJob: Job? = null
    private var currentFileIndex: Int = -1
    private var currentFileId: Long = Long.MIN_VALUE
    private var currentBucketId: Long = Long.MIN_VALUE

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

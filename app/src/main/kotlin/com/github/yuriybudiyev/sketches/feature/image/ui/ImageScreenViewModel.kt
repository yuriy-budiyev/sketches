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

import android.net.Uri
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.data.repository.MediaStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class ImageScreenViewModel @Inject constructor(private val repository: MediaStoreRepository):
    ViewModel() {

    private val uiStateInternal: MutableStateFlow<ImageScreenUiState> =
        MutableStateFlow(ImageScreenUiState.Loading)

    val uiState: StateFlow<ImageScreenUiState>
        get() = uiStateInternal

    fun updateImages(
        fileIndex: Int,
        fileId: Long,
        bucketId: Long,
        silent: Boolean = uiState.value is ImageScreenUiState.Image,
    ) {
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
                    uiStateInternal.value = ImageScreenUiState.Error(e)
                }
            }
        }
    }

    fun updateImages(
        imageIndex: Int,
        bucketId: Long,
    ) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            uiStateInternal.value = ImageScreenUiState.Loading
            try {
                val files = withContext(Dispatchers.Default) { repository.getFiles(bucketId) }
                if (files.isNotEmpty()) {
                    val index = imageIndex.coerceIn(
                        0,
                        files.size - 1
                    )
                    uiStateInternal.value = ImageScreenUiState.Image(
                        index,
                        files[index].id,
                        bucketId,
                        files
                    )
                } else {
                    uiStateInternal.value = ImageScreenUiState.Empty
                }
            } catch (e: Exception) {
                uiStateInternal.value = ImageScreenUiState.Error(e)
            }
        }
    }

    fun deleteImage(
        imageIndex: Int,
        imageUri: Uri,
        bucketId: Long,
    ) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            uiStateInternal.value = ImageScreenUiState.Loading
            try {
                val deleted = withContext(Dispatchers.Default) { repository.deleteFile(imageUri) }
                if (deleted) {
                    val files = withContext(Dispatchers.Default) { repository.getFiles(bucketId) }
                    if (files.isNotEmpty()) {
                        val index = imageIndex.coerceIn(
                            0,
                            files.size - 1
                        )
                        uiStateInternal.value = ImageScreenUiState.Image(
                            index,
                            files[index].id,
                            bucketId,
                            files
                        )
                    } else {
                        uiStateInternal.value = ImageScreenUiState.Empty
                    }
                }
            } catch (e: Exception) {
                uiStateInternal.value = ImageScreenUiState.Error(e)
            }
        }
    }

    private var currentJob: Job? = null
}

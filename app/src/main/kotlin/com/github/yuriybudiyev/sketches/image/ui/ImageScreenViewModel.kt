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

package com.github.yuriybudiyev.sketches.image.ui

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
        imageIndex: Int,
        imageId: Long,
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
                    if (files[imageIndex].id == imageId) {
                        uiStateInternal.value = ImageScreenUiState.Image(
                            imageIndex,
                            imageId,
                            bucketId,
                            files
                        )
                    } else {
                        var backwardIndex = imageIndex - 1
                        var forwardIndex = imageIndex + 1
                        var actualIndex = 0
                        val imagesSize = files.size
                        while (backwardIndex > -1 || forwardIndex < imagesSize) {
                            if (backwardIndex > -1) {
                                if (files[backwardIndex].id == imageId) {
                                    actualIndex = backwardIndex
                                    break
                                }
                                backwardIndex--
                            }
                            if (forwardIndex < imagesSize) {
                                if (files[forwardIndex].id == imageId) {
                                    actualIndex = forwardIndex
                                    break
                                }
                                forwardIndex++
                            }
                        }
                        uiStateInternal.value = ImageScreenUiState.Image(
                            actualIndex,
                            imageId,
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

    private var currentJob: Job? = null
}

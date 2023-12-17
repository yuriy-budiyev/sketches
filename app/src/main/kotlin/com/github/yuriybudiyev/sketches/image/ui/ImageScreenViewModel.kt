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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.images.data.reository.ImagesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImageScreenViewModel @Inject constructor(private val imagesRepository: ImagesRepository):
    ViewModel() {

    val uiState: StateFlow<ImageScreenUiState>
        get() = uiStateInternal

    fun updateImages(
        imagePosition: Int,
        imageId: Long,
        bucketId: Long,
        silent: Boolean = uiState.value is ImageScreenUiState.Image
    ) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            if (!silent) {
                uiStateInternal.value = ImageScreenUiState.Loading
            }
            try {
                val images =
                    withContext(Dispatchers.Default) { imagesRepository.getImages(bucketId) }
                if (!images.isNullOrEmpty()) {
                    if (images[imagePosition].id == imageId) {
                        uiStateInternal.value = ImageScreenUiState.Image(
                            imagePosition,
                            imageId,
                            bucketId,
                            images
                        )
                    } else {
                        var backwardIndex = imagePosition - 1
                        var forwardIndex = imagePosition + 1
                        var actualPosition = 0
                        val imagesSize = images.size
                        while (backwardIndex > -1 || forwardIndex < imagesSize) {
                            if (backwardIndex > -1) {
                                if (images[backwardIndex].id == imageId) {
                                    actualPosition = backwardIndex
                                    break
                                }
                                backwardIndex--
                            }
                            if (forwardIndex < imagesSize) {
                                if (images[forwardIndex].id == imageId) {
                                    actualPosition = forwardIndex
                                    break
                                }
                                forwardIndex++
                            }
                        }
                        uiStateInternal.value = ImageScreenUiState.Image(
                            actualPosition,
                            imageId,
                            bucketId,
                            images
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

    private val uiStateInternal: MutableStateFlow<ImageScreenUiState> =
        MutableStateFlow(ImageScreenUiState.Loading)
    private var currentJob: Job? = null
}

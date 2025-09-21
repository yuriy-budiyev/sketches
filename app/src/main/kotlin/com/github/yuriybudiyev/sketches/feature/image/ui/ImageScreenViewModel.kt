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
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.constants.SketchesConstants
import com.github.yuriybudiyev.sketches.core.coroutines.SketchesCoroutineDispatchers
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.domain.DeleteMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.ui.model.MediaObservingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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
    private val dispatchers: SketchesCoroutineDispatchers,
    private val getMediaFiles: GetMediaFilesUseCase,
    private val deleteMediaFiles: DeleteMediaFilesUseCase,
): MediaObservingViewModel(
    context,
    dispatchers,
) {

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
            bucketId,
        )
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            if (!silent) {
                uiStateInternal.value = ImageScreenUiState.Loading
            }
            try {
                val files = withContext(dispatchers.io) { getMediaFiles(bucketId) }
                val filesSize = files.size
                if (filesSize > 0) {
                    if (fileIndex < filesSize && files[fileIndex].id == fileId) {
                        uiStateInternal.value = ImageScreenUiState.Image(
                            fileIndex = fileIndex,
                            fileId = fileId,
                            bucketId = bucketId,
                            files = files,
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
                            fileIndex = actualIndex.coerceIn(
                                0,
                                filesSize - 1
                            ),
                            fileId = fileId,
                            bucketId = bucketId,
                            files = files,
                        )
                    }
                } else {
                    uiStateInternal.value = ImageScreenUiState.Empty
                }
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                if (!silent) {
                    uiStateInternal.value = ImageScreenUiState.Error(e)
                }
            }
        }
    }

    fun deleteMedia(files: Collection<MediaStoreFile>) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            try {
                withContext(dispatchers.io) {
                    deleteMediaFiles(files)
                }
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                uiStateInternal.value = ImageScreenUiState.Error(e)
            }
        }
    }

    override fun onMediaChanged() {
        updateMediaInternal()
    }

    private var currentJob: Job? = null
    private var currentFileIndex: Int = -1
    private var currentFileId: Long = SketchesConstants.NoId
    private var currentBucketId: Long = SketchesConstants.NoId

    private fun updateMediaInternal() {
        val fileIndex = currentFileIndex
        val fileId = currentFileId
        val bucketId = currentBucketId
        if (fileIndex != -1) {
            updateMedia(
                fileIndex,
                fileId,
                bucketId,
            )
        }
    }
}

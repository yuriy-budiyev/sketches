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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.github.yuriybudiyev.sketches.core.constants.SketchesConstants
import com.github.yuriybudiyev.sketches.core.coroutines.SketchesCoroutineDispatchers
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.domain.DeleteMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.ui.model.MediaObservingViewModel
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImageScreenViewModel @Inject constructor(
    @ApplicationContext
    context: Context,
    savedStateHandle: SavedStateHandle,
    private val dispatchers: SketchesCoroutineDispatchers,
    private val getMediaFiles: GetMediaFilesUseCase,
    private val deleteMediaFiles: DeleteMediaFilesUseCase,
): MediaObservingViewModel(
    context,
    dispatchers,
) {

    private val uiAction: MutableSharedFlow<UiAction> = MutableSharedFlow()
    val uiState: StateFlow<UiState> =
        flow {
            emit(updateMedia())
            uiAction.collect { action ->
                when (action) {
                    is UiAction.UpdateImages -> {
                        emit(updateMedia())
                    }
                    is UiAction.ShowError -> {
                        emit(UiState.Error(action.thrown))
                    }
                }
            }
        }.catch { e ->
            emit(UiState.Error(e))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    private var currentFileIndex: Int = -1
    private var currentFileId: Long = SketchesConstants.NoId
    private var currentBucketId: Long = SketchesConstants.NoId
    fun setCurrentMediaData(
        fileIndex: Int = currentFileIndex,
        fileId: Long = currentFileId,
        bucketId: Long = currentBucketId,
    ) {
        currentFileIndex = fileIndex
        currentFileId = fileId
        currentBucketId = bucketId
    }

    private suspend fun updateMedia(
        fileIndex: Int = currentFileIndex,
        fileId: Long = currentFileId,
        bucketId: Long = currentBucketId,
    ): UiState {
        if (fileIndex == -1) {
            return UiState.Empty
        }
        try {
            val files = withContext(dispatchers.io) { getMediaFiles(bucketId) }
            val filesSize = files.size
            if (filesSize > 0) {
                if (fileIndex < filesSize && files[fileIndex].id == fileId) {
                    return UiState.Image(
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
                    return UiState.Image(
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
                return UiState.Empty
            }
        } catch (e: Exception) {
            return UiState.Error(e)
        }
    }

    private var deleteMediaJob: Job? = null
    fun deleteMedia(files: Collection<MediaStoreFile>) {
        deleteMediaJob?.cancel()
        deleteMediaJob = viewModelScope.launch {
            try {
                withContext(dispatchers.io) {
                    deleteMediaFiles(files)
                }
            } catch (_: CancellationException) {
                // Do nothing
            } catch (e: Exception) {
                uiAction.emit(UiAction.ShowError(e))
            }
        }
    }

    private var onMediaChangedJob: Job? = null
    override fun onMediaChanged() {
        onMediaChangedJob?.cancel()
        onMediaChangedJob = viewModelScope.launch {
            try {
                uiAction.emit(UiAction.UpdateImages)
            } catch (_: CancellationException) {
                // Do nothing
            }
        }
    }

    init {
        val route = savedStateHandle.toRoute<ImageRoute>()
        setCurrentMediaData(
            fileIndex = route.imageIndex,
            fileId = route.imageId,
            bucketId = route.bucketId,
        )
    }

    sealed interface UiState {

        data object Empty: UiState

        data object Loading: UiState

        data class Image(
            val fileIndex: Int,
            val fileId: Long,
            val bucketId: Long,
            val files: List<MediaStoreFile>,
        ): UiState

        data class Error(val thrown: Throwable): UiState
    }

    private sealed interface UiAction {

        data object UpdateImages: UiAction

        data class ShowError(val thrown: Throwable): UiAction
    }
}

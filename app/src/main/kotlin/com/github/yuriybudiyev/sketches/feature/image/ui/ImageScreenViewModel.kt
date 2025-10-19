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
import com.github.yuriybudiyev.sketches.core.coroutines.SketchesCoroutineDispatchers
import com.github.yuriybudiyev.sketches.core.dagger.LazyInject
import com.github.yuriybudiyev.sketches.core.dagger.getValue
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.domain.DeleteMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.flow.WhileSubscribedUi
import com.github.yuriybudiyev.sketches.core.ui.model.MediaObservingViewModel
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
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
    dispatchersLazy: LazyInject<SketchesCoroutineDispatchers>,
    getMediaFilesLazy: LazyInject<GetMediaFilesUseCase>,
    deleteMediaFilesLazy: LazyInject<DeleteMediaFilesUseCase>,
): MediaObservingViewModel(
    context,
    dispatchersLazy,
) {

    private val dispatchers: SketchesCoroutineDispatchers by dispatchersLazy
    private val getMediaFiles: GetMediaFilesUseCase by getMediaFilesLazy
    private val deleteMediaFiles: DeleteMediaFilesUseCase by deleteMediaFilesLazy

    private val uiAction: MutableSharedFlow<UiAction> = MutableSharedFlow()

    val uiState: StateFlow<UiState> =
        flow<UiState> {
            updateMedia()
            uiAction.collect { action ->
                when (action) {
                    is UiAction.UpdateMedia -> {
                        updateMedia()
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
            started = SharingStarted.WhileSubscribedUi(),
            initialValue = UiState.Loading,
        )

    private suspend fun FlowCollector<UiState>.updateMedia(
        fileIndex: Int = currentFileIndex,
        fileId: Long? = currentFileId,
        bucketId: Long? = currentBucketId,
    ) {
        if (fileIndex == -1) {
            emit(UiState.Empty)
            return
        }
        try {
            val files = withContext(dispatchers.io) { getMediaFiles(bucketId) }
            val filesSize = files.size
            if (filesSize > 0) {
                if (fileIndex < filesSize && files[fileIndex].id == fileId) {
                    emit(
                        UiState.Image(
                            index = fileIndex,
                            files = files,
                        ),
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
                    emit(
                        UiState.Image(
                            index = actualIndex.coerceIn(
                                0,
                                filesSize - 1,
                            ),
                            files = files,
                        ),
                    )
                }
            } else {
                emit(UiState.Empty)
            }
        } catch (e: Exception) {
            if (uiState.value !is UiState.Image) {
                emit(UiState.Error(e))
            }
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
                uiAction.emit(UiAction.UpdateMedia)
            } catch (_: CancellationException) {
                // Do nothing
            }
        }
    }

    fun setCurrentFileInfo(
        fileIndex: Int,
        fileId: Long,
    ) {
        currentFileIndex = fileIndex
        currentFileId = fileId
    }

    private var currentFileIndex: Int
    private var currentFileId: Long
    private var currentBucketId: Long?

    init {
        val route = savedStateHandle.toRoute<ImageRoute>()
        currentFileIndex = route.imageIndex
        currentFileId = route.imageId
        currentBucketId = route.bucketId
    }

    sealed interface UiState {

        data object Empty: UiState

        data object Loading: UiState

        data class Image(
            val index: Int,
            val files: List<MediaStoreFile>,
        ): UiState

        data class Error(val thrown: Throwable): UiState
    }

    private sealed interface UiAction {

        data object UpdateMedia: UiAction

        data class ShowError(val thrown: Throwable): UiAction
    }
}

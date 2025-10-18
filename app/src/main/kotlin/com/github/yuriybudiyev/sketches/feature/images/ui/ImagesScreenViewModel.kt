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

package com.github.yuriybudiyev.sketches.feature.images.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.coroutines.SketchesCoroutineDispatchers
import com.github.yuriybudiyev.sketches.core.dagger.LazyInject
import com.github.yuriybudiyev.sketches.core.dagger.getValue
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.domain.DeleteMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.flow.WhileSubscribedUiDefault
import com.github.yuriybudiyev.sketches.core.ui.model.MediaObservingViewModel
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
class ImagesScreenViewModel @Inject constructor(
    @ApplicationContext
    context: Context,
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
            started = SharingStarted.WhileSubscribedUiDefault(),
            initialValue = UiState.Loading,
        )

    private suspend fun FlowCollector<UiState>.updateMedia() {
        try {
            val files = withContext(dispatchers.io) { getMediaFiles() }
            if (files.isNotEmpty()) {
                emit(UiState.Images(files))
            } else {
                emit(UiState.Empty)
            }
        } catch (e: Exception) {
            if (uiState.value !is UiState.Images) {
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

    sealed interface UiState {

        data object Empty: UiState

        data object Loading: UiState

        data class Images(val files: List<MediaStoreFile>): UiState

        data class Error(val thrown: Throwable): UiState
    }

    private sealed interface UiAction {

        data object UpdateMedia: UiAction

        data class ShowError(val thrown: Throwable): UiAction
    }
}

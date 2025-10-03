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

package com.github.yuriybudiyev.sketches.feature.buckets.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.consumable.Consumable
import com.github.yuriybudiyev.sketches.core.coroutines.SketchesCoroutineDispatchers
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.domain.DeleteContentUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetBucketsContentUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetMediaBucketsUseCase
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
class BucketsScreenViewModel @Inject constructor(
    @ApplicationContext
    context: Context,
    private val dispatchers: SketchesCoroutineDispatchers,
    private val getMediaBuckets: GetMediaBucketsUseCase,
    private val getBucketsContent: GetBucketsContentUseCase,
    private val deleteContent: DeleteContentUseCase,
): MediaObservingViewModel(
    context,
    dispatchers,
) {

    private val uiAction: MutableSharedFlow<UiAction> = MutableSharedFlow()

    val uiState: StateFlow<UiState> =
        flow<UiState> {
            updateBuckets()
            uiAction.collect { action ->
                when (action) {
                    is UiAction.UpdateBuckets -> {
                        updateBuckets()
                    }
                    is UiAction.StartSharingBuckets -> {
                        startBucketsAction(action.buckets) { files ->
                            UiState.Buckets.Action.Share(files)
                        }
                    }
                    is UiAction.StartDeletingBuckets -> {
                        startBucketsAction(action.buckets) { files ->
                            UiState.Buckets.Action.Delete(files)
                        }
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
            started = SharingStarted.Lazily,
            initialValue = UiState.Loading,
        )

    private suspend fun FlowCollector<UiState>.updateBuckets() {
        try {
            val buckets = withContext(dispatchers.io) { getMediaBuckets() }
            if (buckets.isNotEmpty()) {
                val oldValue = uiState.value
                if (oldValue is UiState.Buckets) {
                    emit(
                        UiState.Buckets(
                            buckets = buckets,
                            action = oldValue.action,
                        ),
                    )
                } else {
                    emit(
                        UiState.Buckets(
                            buckets = buckets,
                            action = Consumable.consumed(),
                        ),
                    )
                }
            } else {
                emit(UiState.Empty)
            }
        } catch (e: Exception) {
            if (uiState.value !is UiState.Buckets) {
                emit(UiState.Error(e))
            }
        }
    }

    private suspend inline fun FlowCollector<UiState>.startBucketsAction(
        buckets: Collection<MediaStoreBucket>,
        action: (files: List<MediaStoreFile>) -> UiState.Buckets.Action,
    ) {
        try {
            val files = withContext(dispatchers.io) { getBucketsContent(buckets) }
            if (files.isNotEmpty()) {
                val oldState = uiState.value
                if (oldState is UiState.Buckets) {
                    emit(
                        UiState.Buckets(
                            buckets = oldState.buckets,
                            action = Consumable.from(action(files)),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
        }
    }

    private var startSharingBucketsJob: Job? = null

    fun startSharingBuckets(buckets: Collection<MediaStoreBucket>) {
        startSharingBucketsJob?.cancel()
        startSharingBucketsJob = viewModelScope.launch {
            try {
                uiAction.emit(UiAction.StartSharingBuckets(buckets))
            } catch (_: CancellationException) {
                // Do nothing
            }
        }
    }

    private var startDeletingBucketsJob: Job? = null

    fun startDeletingBuckets(buckets: Collection<MediaStoreBucket>) {
        startDeletingBucketsJob?.cancel()
        startDeletingBucketsJob = viewModelScope.launch {
            try {
                uiAction.emit(UiAction.StartDeletingBuckets(buckets))
            } catch (_: CancellationException) {
                // Do nothing
            }
        }
    }

    private var deleteMediaJob: Job? = null

    fun deleteMedia(uris: Collection<String>) {
        deleteMediaJob?.cancel()
        deleteMediaJob = viewModelScope.launch {
            try {
                withContext(dispatchers.io) {
                    deleteContent(uris)
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
                uiAction.emit(UiAction.UpdateBuckets)
            } catch (_: CancellationException) {
                // Do nothing
            }
        }
    }

    sealed interface UiState {

        data object Empty: UiState

        data object Loading: UiState

        data class Buckets(
            val buckets: List<MediaStoreBucket>,
            val action: Consumable<Action>,
        ): UiState {

            sealed interface Action {

                data class Share(val files: List<MediaStoreFile>): Action

                data class Delete(val files: List<MediaStoreFile>): Action
            }
        }

        data class Error(val thrown: Throwable): UiState
    }

    private sealed interface UiAction {

        data object UpdateBuckets: UiAction

        data class StartSharingBuckets(val buckets: Collection<MediaStoreBucket>): UiAction

        data class StartDeletingBuckets(val buckets: Collection<MediaStoreBucket>): UiAction

        data class ShowError(val thrown: Throwable): UiAction
    }
}

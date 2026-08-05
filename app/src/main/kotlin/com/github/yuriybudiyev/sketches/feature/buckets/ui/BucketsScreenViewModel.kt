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

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.consumable.Consumable
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatcher
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatchers
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.domain.DeleteContentUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetBucketsContentUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetMediaBucketsUseCase
import com.github.yuriybudiyev.sketches.core.domain.UpdateMediaAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BucketsScreenViewModel @Inject constructor(
    @Dispatcher(Dispatchers.Default)
    defaultDispatcher: CoroutineDispatcher,
    @Dispatcher(Dispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
    private val getBucketsContent: GetBucketsContentUseCase,
    private val deleteContent: DeleteContentUseCase,
    private val updateMediaAccess: UpdateMediaAccessUseCase,
    getMediaBuckets: GetMediaBucketsUseCase,
): ViewModel() {

    private val action: MutableSharedFlow<Action> = MutableSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> =
        getMediaBuckets().transformLatest { buckets ->
            if (buckets.isEmpty()) {
                emit(UiState.Empty)
            } else {
                val oldState = uiState.value
                if (oldState is UiState.Buckets) {
                    emit(
                        UiState.Buckets(
                            buckets = buckets,
                            action = oldState.action,
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
            }
            action.collect { action ->
                when (action) {
                    is Action.StartSharingBuckets -> {
                        startBucketsAction(action.buckets) { files ->
                            UiState.Buckets.Action.Share(files)
                        }
                    }
                    is Action.StartDeletingBuckets -> {
                        startBucketsAction(action.buckets) { files ->
                            UiState.Buckets.Action.Delete(files)
                        }
                    }
                }
            }
        }.catch { e ->
            emit(UiState.Error(e))
        }.flowOn(defaultDispatcher).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = UiState.Loading,
        )

    private suspend inline fun FlowCollector<UiState>.startBucketsAction(
        buckets: Collection<MediaStoreBucket>,
        action: (files: List<MediaStoreFile>) -> UiState.Buckets.Action,
    ) {
        val files = withContext(ioDispatcher) { getBucketsContent(buckets) }
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
    }

    fun startSharingBuckets(buckets: Collection<MediaStoreBucket>) {
        viewModelScope.launch {
            action.emit(Action.StartSharingBuckets(buckets))
        }
    }

    fun startDeletingBuckets(buckets: Collection<MediaStoreBucket>) {
        viewModelScope.launch {
            action.emit(Action.StartDeletingBuckets(buckets))
        }
    }

    fun deleteMedia(uris: Collection<Uri>) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                deleteContent(uris)
            }
        }
    }

    fun updateMediaAccess() {
        viewModelScope.launch {
            updateMediaAccess.invoke()
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

    private sealed interface Action {

        data class StartSharingBuckets(val buckets: Collection<MediaStoreBucket>): Action

        data class StartDeletingBuckets(val buckets: Collection<MediaStoreBucket>): Action
    }
}

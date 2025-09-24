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
import com.github.yuriybudiyev.sketches.core.domain.GetBucketsContentUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetMediaBucketsUseCase
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
class BucketsScreenViewModel @Inject constructor(
    @ApplicationContext
    context: Context,
    private val dispatchers: SketchesCoroutineDispatchers,
    private val getMediaBuckets: GetMediaBucketsUseCase,
    private val getBucketsContent: GetBucketsContentUseCase,
): MediaObservingViewModel(
    context,
    dispatchers,
) {

    private val uiStateInternal: MutableStateFlow<BucketsScreenUiState> =
        MutableStateFlow(BucketsScreenUiState.Loading)

    val uiState: StateFlow<BucketsScreenUiState>
        get() = uiStateInternal

    fun updateBuckets(silent: Boolean = uiState.value is BucketsScreenUiState.Buckets) {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            if (!silent) {
                uiStateInternal.value = BucketsScreenUiState.Loading
            }
            try {
                val buckets = withContext(dispatchers.io) { getMediaBuckets() }
                if (buckets.isNotEmpty()) {
                    val oldValue = uiStateInternal.value
                    if (oldValue is BucketsScreenUiState.Buckets) {
                        uiStateInternal.value = BucketsScreenUiState.Buckets(
                            buckets = buckets,
                            action = oldValue.action,
                        )
                    } else {
                        uiStateInternal.value = BucketsScreenUiState.Buckets(
                            buckets = buckets,
                            action = Consumable.consumed(),
                        )
                    }
                } else {
                    uiStateInternal.value = BucketsScreenUiState.Empty
                }
            } catch (_: CancellationException) {
                // Do nothing
            } catch (e: Exception) {
                if (!silent) {
                    uiStateInternal.value = BucketsScreenUiState.Error(e)
                }
            }
        }
    }

    private inline fun startBucketsAction(
        buckets: Collection<MediaStoreBucket>,
        crossinline action: (files: List<MediaStoreFile>) -> BucketsScreenUiState.Buckets.Action,
    ) {
        actionJob?.cancel()
        actionJob = viewModelScope.launch {
            try {
                val files = withContext(dispatchers.io) { getBucketsContent(buckets) }
                if (files.isNotEmpty()) {
                    val oldState = uiStateInternal.value
                    if (oldState is BucketsScreenUiState.Buckets) {
                        uiStateInternal.value = BucketsScreenUiState.Buckets(
                            buckets = oldState.buckets,
                            action = Consumable.from(action(files)),
                        )
                    }
                }
            } catch (_: CancellationException) {
                // Do nothing
            } catch (_: Exception) {
                // Do nothing
            }
        }
    }

    fun startSharingBuckets(buckets: Collection<MediaStoreBucket>) {
        startBucketsAction(buckets) { files -> BucketsScreenUiState.Buckets.Action.Share(files) }
    }

    fun startDeletingBuckets(buckets: Collection<MediaStoreBucket>) {
        startBucketsAction(buckets) { files -> BucketsScreenUiState.Buckets.Action.Delete(files) }
    }

    override fun onMediaChanged() {
        updateBuckets()
    }

    private var updateJob: Job? = null
    private var actionJob: Job? = null
}

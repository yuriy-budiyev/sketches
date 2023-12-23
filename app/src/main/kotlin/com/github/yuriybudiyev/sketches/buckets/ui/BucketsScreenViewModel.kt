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

package com.github.yuriybudiyev.sketches.buckets.ui

import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.buckets.data.repository.BucketsRepository
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class BucketsScreenViewModel @Inject constructor(private val bucketsRepository: BucketsRepository):
    ViewModel() {

    val uiState: StateFlow<BucketsScreenUiState>
        get() = uiStateInternal

    fun updateBuckets(silent: Boolean = uiState.value is BucketsScreenUiState.Buckets) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            if (!silent) {
                uiStateInternal.value = BucketsScreenUiState.Loading
            }
            try {
                val buckets = withContext(Dispatchers.Default) {
                    bucketsRepository.getBuckets()
                }
                if (!buckets.isNullOrEmpty()) {
                    uiStateInternal.value = BucketsScreenUiState.Buckets(buckets)
                } else {
                    if (!silent) {
                        uiStateInternal.value = BucketsScreenUiState.Empty
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    uiStateInternal.value = BucketsScreenUiState.Error(e)
                }
            }
        }
    }

    private val uiStateInternal: MutableStateFlow<BucketsScreenUiState> =
        MutableStateFlow(BucketsScreenUiState.Loading)
    private var currentJob: Job? = null
}

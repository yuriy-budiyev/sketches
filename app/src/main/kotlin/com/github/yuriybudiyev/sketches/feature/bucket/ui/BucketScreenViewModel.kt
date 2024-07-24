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

package com.github.yuriybudiyev.sketches.feature.bucket.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.constants.SketchesConstants
import com.github.yuriybudiyev.sketches.core.domain.GetMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.ui.model.MediaObservingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BucketScreenViewModel @Inject constructor(
    @ApplicationContext
    context: Context,
    private val getMediaFiles: GetMediaFilesUseCase,
): MediaObservingViewModel(context) {

    private val uiStateInternal: MutableStateFlow<BucketScreenUiState> =
        MutableStateFlow(BucketScreenUiState.Loading)

    val uiState: StateFlow<BucketScreenUiState>
        get() = uiStateInternal

    fun updateMedia(
        bucketId: Long = currentBucketId,
        silent: Boolean = uiState.value is BucketScreenUiState.Bucket,
    ) {
        currentBucketId = bucketId
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            if (!silent) {
                uiStateInternal.value = BucketScreenUiState.Loading
            }
            try {
                val files = withContext(Dispatchers.Default) { getMediaFiles(bucketId) }
                if (files.isNotEmpty()) {
                    uiStateInternal.value = BucketScreenUiState.Bucket(
                        bucketId,
                        files
                    )
                } else {
                    uiStateInternal.value = BucketScreenUiState.Empty
                }
            } catch (_: CancellationException) {
                // Do nothing
            } catch (e: Exception) {
                if (!silent) {
                    uiStateInternal.value = BucketScreenUiState.Error(e)
                }
            }
        }
    }

    override fun onMediaChanged() {
        updateMedia()
    }

    private var currentJob: Job? = null
    private var currentBucketId: Long = SketchesConstants.NoId
}

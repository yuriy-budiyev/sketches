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

package com.github.yuriybudiyev.sketches.gallery.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.gallery.data.reository.GalleryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class GalleryScreenViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: GalleryRepository
): ViewModel() {

    val uiState: StateFlow<GalleryScreenUiState>
        get() = uiStateInternal

    fun setNoPermission() {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            uiStateInternal.value = GalleryScreenUiState.NoPermission
        }
    }

    fun updateImages() {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            uiStateInternal.value = GalleryScreenUiState.Loading
            try {
                val images = withContext(Dispatchers.Default) { repository.getImages() }
                if (!images.isNullOrEmpty()) {
                    uiStateInternal.value = GalleryScreenUiState.Gallery(images)
                } else {
                    uiStateInternal.value = GalleryScreenUiState.Empty
                }
            } catch (e: Exception) {
                uiStateInternal.value = GalleryScreenUiState.Error(e)
            }
        }
    }

    private val uiStateInternal: MutableStateFlow<GalleryScreenUiState> =
        MutableStateFlow(GalleryScreenUiState.Empty)
    private var currentJob: Job? = null
}

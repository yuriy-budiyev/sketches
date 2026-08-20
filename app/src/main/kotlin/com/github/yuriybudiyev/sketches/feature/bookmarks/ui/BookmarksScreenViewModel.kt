/*
 * MIT License
 *
 * Copyright (c) 2026 Yuriy Budiyev
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

package com.github.yuriybudiyev.sketches.feature.bookmarks.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatcher
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatchers
import com.github.yuriybudiyev.sketches.core.data.model.MediaFile
import com.github.yuriybudiyev.sketches.core.domain.DeleteBookmarksUseCase
import com.github.yuriybudiyev.sketches.core.domain.DeleteMediaUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetBookmarksUseCase
import com.github.yuriybudiyev.sketches.core.domain.UpdateMediaAccessUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksScreenViewModel @Inject constructor(
    @Dispatcher(Dispatchers.Default)
    defaultDispatcher: CoroutineDispatcher,
    private val deleteMedia: DeleteMediaUseCase,
    private val deleteBookmarks: DeleteBookmarksUseCase,
    private val updateMediaAccess: UpdateMediaAccessUseCase,
    getBookmarks: GetBookmarksUseCase,
): ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> = getBookmarks()
        .transformLatest { bookmarks ->
            if (bookmarks.isEmpty()) {
                emit(UiState.Empty)
            } else {
                emit(UiState.Bookmarks(bookmarks))
            }
        }.catch { thrown ->
            emit(UiState.Error(thrown))
        }.flowOn(defaultDispatcher).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = UiState.Loading,
        )

    fun deleteMedia(files: Collection<Uri>) {
        viewModelScope.launch {
            deleteMedia.invoke(files)
        }
    }

    fun deleteBookmarks(mediaIds: Collection<Long>) {
        viewModelScope.launch {
            deleteBookmarks.invoke(mediaIds)
        }
    }

    fun updateMediaAccess() {
        viewModelScope.launch {
            updateMediaAccess.invoke()
        }
    }

    sealed interface UiState {

        data class Bookmarks(val files: List<MediaFile>): UiState

        data class Error(val thrown: Throwable): UiState

        data object Empty: UiState

        data object Loading: UiState
    }
}

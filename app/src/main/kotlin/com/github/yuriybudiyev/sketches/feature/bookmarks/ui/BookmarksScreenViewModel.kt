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

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatcher
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatchers
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.domain.GetBookmarksUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.ui.model.MediaObservingViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BookmarksScreenViewModel @Inject constructor(
    @ApplicationContext
    context: Context,
    @Dispatcher(Dispatchers.Default)
    defaultDispatcher: CoroutineDispatcher,
    @Dispatcher(Dispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
    private val getMediaFiles: GetMediaFilesUseCase,
    getBookmarks: GetBookmarksUseCase,
): MediaObservingViewModel(context) {

    private val action: MutableSharedFlow<Action> = MutableSharedFlow()

    val uiState: StateFlow<UiState> =
        flow {
            updateFiles()
            action.collect { action ->
                when (action) {
                    is Action.UpdateMedia -> {
                        updateFiles()
                    }
                }
            }
        }.combineTransform(getBookmarks()) { state, bookmarks ->
            when (state) {
                is IntermediateState.Items -> {
                    throw IllegalStateException("Forbidden state: $state")
                }
                is IntermediateState.Files -> {
                    val items = ArrayList<BookmarkItem>(bookmarks.size)
                    for (file in state.files) {
                        val bookmark = bookmarks[file.id]
                        if (bookmark != null) {
                            items.add(
                                BookmarkItem(
                                    file = file,
                                    bookmark = bookmark,
                                ),
                            )
                        }
                    }
                    items.sortByDescending { item -> item.bookmark.dateAdded }
                    emit(IntermediateState.Items(items))
                }
                is IntermediateState.Empty -> {
                    emit(state)
                }
            }
        }.transform { state ->
            when (state) {
                is IntermediateState.Items -> {
                    emit(UiState.Bookmarks(state.items))
                }
                is IntermediateState.Files -> {
                    throw IllegalStateException("Forbidden state: $state")
                }
                is IntermediateState.Empty -> {
                    emit(UiState.Empty)
                }
            }
        }.catch { thrown ->
            emit(UiState.Error(thrown))
        }.flowOn(defaultDispatcher).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = UiState.Loading,
        )

    override suspend fun onMediaChanged() {
        action.emit(Action.UpdateMedia)
    }

    private suspend fun FlowCollector<IntermediateState>.updateFiles() {
        val files = withContext(ioDispatcher) { getMediaFiles() }
        if (files.isNotEmpty()) {
            emit(IntermediateState.Files(files))
        } else {
            emit(IntermediateState.Empty)
        }
    }

    sealed interface UiState {

        data class Bookmarks(val bookmarks: List<BookmarkItem>): UiState

        data class Error(val thrown: Throwable): UiState

        data object Empty: UiState

        data object Loading: UiState
    }

    private sealed interface IntermediateState {

        data class Files(val files: List<MediaStoreFile>): IntermediateState

        data class Items(val items: List<BookmarkItem>): IntermediateState

        data object Empty: IntermediateState
    }

    private sealed interface Action {

        data object UpdateMedia: Action
    }
}

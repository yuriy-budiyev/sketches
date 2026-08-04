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
import com.github.yuriybudiyev.sketches.core.data.model.Bookmark
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.domain.DeleteBookmarksUseCase
import com.github.yuriybudiyev.sketches.core.domain.DeleteMediaUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetBookmarksUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.domain.UpdateMediaUseCase
import com.github.yuriybudiyev.sketches.core.ui.model.SketchesViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val deleteMedia: DeleteMediaUseCase,
    private val updateMedia: UpdateMediaUseCase,
    private val deleteBookmarks: DeleteBookmarksUseCase,
    getMediaFiles: GetMediaFilesUseCase,
    getBookmarks: GetBookmarksUseCase,
): SketchesViewModel(context) {

    val uiState: StateFlow<UiState> = combineTransform(
        getMediaFiles(),
        getBookmarks(),
    ) { files, bookmarks ->
        if (files.isEmpty() || bookmarks.isEmpty()) {
            emit(UiState.Empty)
        } else {
            val temp = ArrayList<FileWithBookmark>(bookmarks.size)
            for (file in files) {
                val bookmark = bookmarks[file.id]
                if (bookmark != null) {
                    temp.add(
                        FileWithBookmark(
                            file = file,
                            bookmark = bookmark,
                        ),
                    )
                }
            }
            if (temp.isEmpty()) {
                emit(UiState.Empty)
            } else {
                temp.sortByDescending { item -> item.bookmark.dateAdded }
                emit(UiState.Bookmarks(temp.map { item -> item.file }))
            }
        }
    }.catch { thrown ->
        emit(UiState.Error(thrown))
    }.flowOn(defaultDispatcher).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = UiState.Loading,
    )

    fun deleteMedia(files: Collection<MediaStoreFile>) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                deleteMedia.invoke(files)
            }
        }
    }

    fun deleteBookmarks(mediaIds: Collection<Long>) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                deleteBookmarks.invoke(mediaIds)
            }
        }
    }

    override fun onMediaAccessChanged() {
        updateMedia()
    }

    sealed interface UiState {

        data class Bookmarks(val files: List<MediaStoreFile>): UiState

        data class Error(val thrown: Throwable): UiState

        data object Empty: UiState

        data object Loading: UiState
    }

    private data class FileWithBookmark(
        val file: MediaStoreFile,
        val bookmark: Bookmark,
    )
}

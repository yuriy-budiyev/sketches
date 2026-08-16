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

package com.github.yuriybudiyev.sketches.feature.image.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatcher
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatchers
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.domain.CreateBookmarkUseCase
import com.github.yuriybudiyev.sketches.core.domain.DeleteBookmarksUseCase
import com.github.yuriybudiyev.sketches.core.domain.DeleteMediaUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetBookmarksUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.domain.UpdateMediaAccessUseCase
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageNavRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ImageScreenViewModel.Factory::class)
class ImageScreenViewModel @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    @Assisted
    route: ImageNavRoute,
    @Dispatcher(Dispatchers.Default)
    defaultDispatcher: CoroutineDispatcher,
    private val deleteMedia: DeleteMediaUseCase,
    private val createBookmark: CreateBookmarkUseCase,
    private val deleteBookmarks: DeleteBookmarksUseCase,
    private val updateMediaAccess: UpdateMediaAccessUseCase,
    getMediaFiles: GetMediaFilesUseCase,
    getBookmarks: GetBookmarksUseCase,
): ViewModel() {

    val uiState: StateFlow<UiState>

    private suspend fun FlowCollector<UiState>.checkIndexAndEmitItems(
        items: List<MediaStoreFile>,
        fileIndex: Int = currentFileIndex,
        fileId: Long? = currentFileId,
    ) {
        val itemsSize = items.size
        if (fileIndex < itemsSize && items[fileIndex].id == fileId) {
            emit(
                UiState.Image(
                    files = items,
                    index = fileIndex,
                ),
            )
        } else {
            var backwardIndex = fileIndex - 1
            var forwardIndex = fileIndex + 1
            var actualIndex = fileIndex
            while (backwardIndex > -1 || forwardIndex < itemsSize) {
                if (backwardIndex > -1) {
                    if (items[backwardIndex].id == fileId) {
                        actualIndex = backwardIndex
                        break
                    }
                    backwardIndex--
                }
                if (forwardIndex < itemsSize) {
                    if (items[forwardIndex].id == fileId) {
                        actualIndex = forwardIndex
                        break
                    }
                    forwardIndex++
                }
            }
            emit(
                UiState.Image(
                    files = items,
                    index = actualIndex.coerceIn(
                        minimumValue = 0,
                        maximumValue = itemsSize - 1,
                    ),
                ),
            )
        }
    }

    fun deleteMedia(files: Collection<Uri>) {
        viewModelScope.launch {
            deleteMedia.invoke(files)
        }
    }

    fun updateMediaAccess() {
        viewModelScope.launch {
            updateMediaAccess.invoke()
        }
    }

    fun createBookmark(mediaId: Long) {
        viewModelScope.launch {
            createBookmark.invoke(mediaId)
        }
    }

    fun deleteBookmark(mediaId: Long) {
        viewModelScope.launch {
            deleteBookmarks.invoke(listOf(mediaId))
        }
    }

    fun setCurrentFileInfo(
        fileIndex: Int,
        fileId: Long,
    ) {
        currentFileIndex = fileIndex
        savedStateHandle[Keys.CurrentFileIndex] = fileIndex
        currentFileId = fileId
        savedStateHandle[Keys.CurrentFileId] = fileId
    }

    var currentFileIndex: Int
        private set

    var currentFileId: Long
        private set

    private val currentBucketId: Long?
    private val mode: Mode

    init {
        when (val data = route.data) {
            is ImageNavRoute.Data.Images -> {
                currentFileIndex = savedStateHandle[Keys.CurrentFileIndex] ?: data.imageIndex
                currentFileId = savedStateHandle[Keys.CurrentFileId] ?: data.imageId
                currentBucketId = null
                mode = Mode.Images
            }
            is ImageNavRoute.Data.Bookmarks -> {
                currentFileIndex = savedStateHandle[Keys.CurrentFileIndex] ?: data.imageIndex
                currentFileId = savedStateHandle[Keys.CurrentFileId] ?: data.imageId
                currentBucketId = null
                mode = Mode.Bookmarks
            }
            is ImageNavRoute.Data.Bucket -> {
                currentFileIndex = savedStateHandle[Keys.CurrentFileIndex] ?: data.imageIndex
                currentFileId = savedStateHandle[Keys.CurrentFileId] ?: data.imageId
                currentBucketId = data.bucketId
                mode = Mode.Images
            }
        }
        @OptIn(ExperimentalCoroutinesApi::class)
        uiState = when (mode) {
            Mode.Images -> getMediaFiles(currentBucketId)
            Mode.Bookmarks -> getBookmarks()
        }.transformLatest { files ->
            if (files.isEmpty()) {
                emit(UiState.Empty)
            } else {
                checkIndexAndEmitItems(files)
            }
        }.catch { t ->
            emit(UiState.Error(t))
        }.flowOn(defaultDispatcher).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = UiState.Loading,
        )
    }

    sealed interface UiState {

        data object Empty: UiState

        data object Loading: UiState

        data class Image(
            val files: List<MediaStoreFile>,
            val index: Int,
        ): UiState

        data class Error(val thrown: Throwable): UiState
    }

    private enum class Mode {

        Images,
        Bookmarks,
    }

    @AssistedFactory
    interface Factory {

        fun create(route: ImageNavRoute): ImageScreenViewModel
    }

    private object Keys {

        const val CurrentFileIndex: String = "current_file_index"
        const val CurrentFileId: String = "current_file_id"
    }
}

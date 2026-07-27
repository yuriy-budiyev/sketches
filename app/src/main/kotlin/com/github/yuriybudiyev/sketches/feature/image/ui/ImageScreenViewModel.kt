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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatcher
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatchers
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.domain.CreateBookmarkUseCase
import com.github.yuriybudiyev.sketches.core.domain.DeleteBookmarkUseCase
import com.github.yuriybudiyev.sketches.core.domain.DeleteMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetBookmarksUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.ui.model.MediaObservingViewModel
import com.github.yuriybudiyev.sketches.feature.image.navigation.ImageNavRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = ImageScreenViewModel.Factory::class)
class ImageScreenViewModel @AssistedInject constructor(
    @ApplicationContext
    context: Context,
    private val savedStateHandle: SavedStateHandle,
    @Assisted
    route: ImageNavRoute,
    @Dispatcher(Dispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
    private val getMediaFiles: GetMediaFilesUseCase,
    private val deleteMediaFiles: DeleteMediaFilesUseCase,
    private val createBookmark: CreateBookmarkUseCase,
    private val deleteBookmark: DeleteBookmarkUseCase,
    getBookmarks: GetBookmarksUseCase,
): MediaObservingViewModel(context) {

    private val uiAction: MutableSharedFlow<UiAction> = MutableSharedFlow()

    val uiState: StateFlow<UiState> =
        flow<IntermediateState> {
            updateFiles()
            uiAction.collect { action ->
                when (action) {
                    is UiAction.UpdateMedia -> {
                        updateFiles()
                    }
                    is UiAction.ShowError -> {
                        emit(IntermediateState.Error(action.thrown))
                    }
                }
            }
        }.combineTransform(getBookmarks()) { state, bookmarks ->
            if (state is IntermediateState.Files) {
                emitItemsWithIndex(
                    when (mode) {
                        Mode.Images -> {
                            state.files.map { file ->
                                MediaItem(
                                    file = file,
                                    isMarked = bookmarks.containsKey(file.id),
                                )
                            }
                        }
                        Mode.Bookmarks -> {
                            val files = state.files
                            if (files.isEmpty()) {
                                emptyList()
                            } else {
                                state.files.asSequence()
                                    .filter { file -> bookmarks.containsKey(file.id) }
                                    .mapTo(ArrayList(bookmarks.size)) { file ->
                                        MediaItem(
                                            file = file,
                                            isMarked = true,
                                        )
                                    }
                            }
                        }
                    },
                )
            } else {
                emit(state)
            }
        }.transform { state ->
            when (state) {
                is IntermediateState.ItemsWithIndex -> {
                    emit(
                        UiState.Image(
                            items = state.items,
                            index = state.index,
                        ),
                    )
                }
                is IntermediateState.Files -> {
                    throw IllegalStateException("Invalid state: $state")
                }
                is IntermediateState.Empty -> {
                    emit(UiState.Empty)
                }
                is IntermediateState.Error -> {
                    emit(UiState.Error(state.thrown))
                }
            }
        }.catch { e ->
            emit(UiState.Error(e))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = UiState.Loading,
        )

    private suspend fun FlowCollector<IntermediateState>.updateFiles(
        bucketId: Long? = currentBucketId,
    ) {
        try {
            val files = withContext(ioDispatcher) { getMediaFiles(bucketId) }
            if (files.isNotEmpty()) {
                emit(IntermediateState.Files(files))
            } else {
                emit(IntermediateState.Empty)
            }
        } catch (_: CancellationException) {
            // Do nothing
        } catch (e: Exception) {
            if (uiState.value !is UiState.Image) {
                emit(IntermediateState.Error(e))
            }
        }
    }

    private suspend fun FlowCollector<IntermediateState>.emitItemsWithIndex(
        items: List<MediaItem>,
        fileIndex: Int = currentFileIndex,
        fileId: Long? = currentFileId,
    ) {
        val filesSize = items.size
        if (fileIndex < filesSize && items[fileIndex].file.id == fileId) {
            emit(
                IntermediateState.ItemsWithIndex(
                    items = items,
                    index = fileIndex,
                ),
            )
        } else {
            var backwardIndex = fileIndex - 1
            var forwardIndex = fileIndex + 1
            var actualIndex = fileIndex
            while (backwardIndex > -1 || forwardIndex < filesSize) {
                if (backwardIndex > -1) {
                    if (items[backwardIndex].file.id == fileId) {
                        actualIndex = backwardIndex
                        break
                    }
                    backwardIndex--
                }
                if (forwardIndex < filesSize) {
                    if (items[forwardIndex].file.id == fileId) {
                        actualIndex = forwardIndex
                        break
                    }
                    forwardIndex++
                }
            }
            val removedFiles = (uiState.value as? UiState.Image)?.let { state ->
                state.items.size - filesSize
            } ?: 0
            emit(
                IntermediateState.ItemsWithIndex(
                    items = items,
                    index = if (backwardIndex == -1 && forwardIndex == filesSize && removedFiles > 1) {
                        0
                    } else {
                        actualIndex.coerceIn(
                            0,
                            filesSize - 1,
                        )
                    },
                ),
            )
        }
    }

    fun deleteMedia(files: Collection<MediaStoreFile>) {
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    deleteMediaFiles(files)
                }
            } catch (_: CancellationException) {
                // Do nothing
            } catch (e: Exception) {
                uiAction.emit(UiAction.ShowError(e))
            }
        }
    }

    override suspend fun onMediaChanged() {
        uiAction.emit(UiAction.UpdateMedia)
    }

    fun createBookmark(mediaId: Long) {
        viewModelScope.launch {
            createBookmark.invoke(mediaId)
        }
    }

    fun deleteBookmark(mediaId: Long) {
        viewModelScope.launch {
            deleteBookmark.invoke(mediaId)
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
    }

    sealed interface UiState {

        data object Empty: UiState

        data object Loading: UiState

        data class Image(
            val items: List<MediaItem>,
            val index: Int,
        ): UiState

        data class Error(val thrown: Throwable): UiState
    }

    private sealed interface IntermediateState {

        data class ItemsWithIndex(
            val items: List<MediaItem>,
            val index: Int,
        ): IntermediateState

        data class Files(val files: List<MediaStoreFile>): IntermediateState

        data class Error(val thrown: Throwable): IntermediateState

        data object Empty: IntermediateState
    }

    private sealed interface UiAction {

        data object UpdateMedia: UiAction

        data class ShowError(val thrown: Throwable): UiAction
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

        const val CurrentFileIndex = "current_file_index"
        const val CurrentFileId = "current_file_id"
    }
}

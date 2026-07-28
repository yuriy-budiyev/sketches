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
import com.github.yuriybudiyev.sketches.core.data.model.Bookmark
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = ImageScreenViewModel.Factory::class)
class ImageScreenViewModel @AssistedInject constructor(
    @ApplicationContext
    context: Context,
    private val savedStateHandle: SavedStateHandle,
    @Assisted
    route: ImageNavRoute,
    @Dispatcher(Dispatchers.Default)
    defaultDispatcher: CoroutineDispatcher,
    @Dispatcher(Dispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
    private val getMediaFiles: GetMediaFilesUseCase,
    private val deleteMediaFiles: DeleteMediaFilesUseCase,
    private val createBookmark: CreateBookmarkUseCase,
    private val deleteBookmark: DeleteBookmarkUseCase,
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
                    val files = state.files
                    when (mode) {
                        Mode.Images -> {
                            checkIndexAndEmitItems(
                                files.map { file ->
                                    ImageItem(
                                        file = file,
                                        isMarked = bookmarks.containsKey(file.id),
                                    )
                                },
                            )
                        }
                        Mode.Bookmarks -> {
                            if (files.isEmpty() || bookmarks.isEmpty()) {
                                emit(IntermediateState.Empty)
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
                                    emit(IntermediateState.Empty)
                                } else {
                                    temp.sortByDescending { item -> item.bookmark.dateAdded }
                                    checkIndexAndEmitItems(
                                        temp.map { item ->
                                            ImageItem(
                                                file = item.file,
                                                isMarked = true,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                is IntermediateState.Empty -> {
                    emit(state)
                }
            }
        }.transform { state ->
            when (state) {
                is IntermediateState.Items -> {
                    emit(
                        UiState.Image(
                            items = state.items,
                            index = state.index,
                        ),
                    )
                }
                is IntermediateState.Files -> {
                    throw IllegalStateException("Forbidden state: $state")
                }
                is IntermediateState.Empty -> {
                    emit(UiState.Empty)
                }
            }
        }.catch { t ->
            emit(UiState.Error(t))
        }.flowOn(defaultDispatcher).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = UiState.Loading,
        )

    private suspend fun FlowCollector<IntermediateState>.updateFiles(
        bucketId: Long? = currentBucketId,
    ) {
        val files = withContext(ioDispatcher) { getMediaFiles(bucketId) }
        if (files.isNotEmpty()) {
            emit(IntermediateState.Files(files))
        } else {
            emit(IntermediateState.Empty)
        }
    }

    private suspend fun FlowCollector<IntermediateState>.checkIndexAndEmitItems(
        items: List<ImageItem>,
        fileIndex: Int = currentFileIndex,
        fileId: Long? = currentFileId,
    ) {
        val itemsSize = items.size
        if (fileIndex < itemsSize && items[fileIndex].file.id == fileId) {
            emit(
                IntermediateState.Items(
                    items = items,
                    index = fileIndex,
                ),
            )
        } else {
            var backwardIndex = fileIndex - 1
            var forwardIndex = fileIndex + 1
            var actualIndex = fileIndex
            while (backwardIndex > -1 || forwardIndex < itemsSize) {
                if (backwardIndex > -1) {
                    if (items[backwardIndex].file.id == fileId) {
                        actualIndex = backwardIndex
                        break
                    }
                    backwardIndex--
                }
                if (forwardIndex < itemsSize) {
                    if (items[forwardIndex].file.id == fileId) {
                        actualIndex = forwardIndex
                        break
                    }
                    forwardIndex++
                }
            }
            emit(
                IntermediateState.Items(
                    items = items,
                    index = actualIndex.coerceIn(
                        0,
                        itemsSize - 1,
                    ),
                ),
            )
        }
    }

    fun deleteMedia(files: Collection<MediaStoreFile>) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                deleteMediaFiles(files)
            }
        }
    }

    override suspend fun onMediaChanged() {
        action.emit(Action.UpdateMedia)
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
            val items: List<ImageItem>,
            val index: Int,
        ): UiState

        data class Error(val thrown: Throwable): UiState
    }

    private sealed interface IntermediateState {

        data class Items(
            val items: List<ImageItem>,
            val index: Int,
        ): IntermediateState

        data class Files(val files: List<MediaStoreFile>): IntermediateState

        data object Empty: IntermediateState
    }

    private sealed interface Action {

        data object UpdateMedia: Action
    }

    private enum class Mode {

        Images,
        Bookmarks,
    }

    private data class FileWithBookmark(
        val file: MediaStoreFile,
        val bookmark: Bookmark,
    )

    @AssistedFactory
    interface Factory {

        fun create(route: ImageNavRoute): ImageScreenViewModel
    }

    private object Keys {

        const val CurrentFileIndex = "current_file_index"
        const val CurrentFileId = "current_file_id"
    }
}

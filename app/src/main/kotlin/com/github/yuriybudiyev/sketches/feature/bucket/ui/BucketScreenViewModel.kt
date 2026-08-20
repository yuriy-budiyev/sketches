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

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.domain.DeleteMediaUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetAllMediaFilesUseCase
import com.github.yuriybudiyev.sketches.core.domain.GetHiddenBucketsUseCase
import com.github.yuriybudiyev.sketches.core.domain.HideBucketUseCase
import com.github.yuriybudiyev.sketches.core.domain.ShowBucketUseCase
import com.github.yuriybudiyev.sketches.core.domain.UpdateMediaAccessUseCase
import com.github.yuriybudiyev.sketches.feature.bucket.navigation.BucketNavRoute
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = BucketScreenViewModel.Factory::class)
class BucketScreenViewModel @AssistedInject constructor(
    @Assisted
    route: BucketNavRoute,
    private val deleteMedia: DeleteMediaUseCase,
    private val updateMediaAccess: UpdateMediaAccessUseCase,
    private val showBucket: ShowBucketUseCase,
    private val hideBucket: HideBucketUseCase,
    getMediaFiles: GetAllMediaFilesUseCase,
    getHiddenBuckets: GetHiddenBucketsUseCase,
): ViewModel() {

    val bucketId: Long = route.bucketId
    val bucketName: String = route.bucketName

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> =
        combineTransform(
            getMediaFiles(bucketId),
            getHiddenBuckets(),
        ) { files, buckets ->
            emit(files to buckets)
        }.transformLatest { (files, buckets) ->
            if (files.isNotEmpty()) {
                emit(
                    UiState.Bucket(
                        files = files,
                        isVisible = !buckets.contains(bucketId),
                    ),
                )
            } else {
                emit(UiState.Empty)
            }
        }.catch { e ->
            emit(UiState.Error(e))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = UiState.Loading,
        )

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

    fun showBucket() {
        viewModelScope.launch {
            showBucket(bucketId)
        }
    }

    fun hideBucket() {
        viewModelScope.launch {
            hideBucket(bucketId)
        }
    }

    sealed interface UiState {

        data object Empty: UiState

        data object Loading: UiState

        data class Bucket(
            val files: List<MediaStoreFile>,
            val isVisible: Boolean,
        ): UiState

        data class Error(val thrown: Throwable): UiState
    }

    @AssistedFactory
    interface Factory {

        fun create(route: BucketNavRoute): BucketScreenViewModel
    }
}

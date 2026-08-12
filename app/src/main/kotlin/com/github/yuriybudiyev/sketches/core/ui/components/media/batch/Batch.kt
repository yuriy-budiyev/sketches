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

package com.github.yuriybudiyev.sketches.core.ui.components.media.batch

import android.content.ContentUris
import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.github.yuriybudiyev.sketches.core.collections.newLinkedHashSet
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.parcelize.Parcelize

@Composable
fun rememberMediaBatchState(): MediaBatchState =
    rememberSaveable(saver = MediaBatchStateImplSaver) { MediaBatchStateImpl() }

fun MediaStoreFile.toMediaDescriptor(): MediaDescriptor =
    MediaDescriptor(
        id = id,
        type = mediaType.ordinal,
    )

fun Collection<MediaStoreFile>.toUriList(filterIds: Set<Long>): List<Uri> {
    val size = this.size.coerceAtMost(filterIds.size)
    val uris = ArrayList<Uri>(size)
    for (file in this) {
        if (filterIds.contains(file.id)) {
            uris.add(file.uri)
        }
        if (uris.size == size) {
            break
        }
    }
    return uris
}

fun Collection<MediaStoreFile>.toMediaList(): List<MediaDescriptor> =
    mapTo(ArrayList(size)) { file -> file.toMediaDescriptor() }

fun Collection<MediaStoreFile>.toMediaList(filterIds: Collection<Long>): List<MediaDescriptor> {
    val size = this.size.coerceAtMost(filterIds.size)
    val media = ArrayList<MediaDescriptor>(size)
    for (file in this) {
        if (filterIds.contains(file.id)) {
            media.add(file.toMediaDescriptor())
        }
        if (media.size == size) {
            break
        }
    }
    return media
}

fun MediaDescriptor.toUri(): Uri =
    ContentUris.withAppendedId(
        MediaType.entries[type].contentUri,
        id,
    )

fun Collection<MediaDescriptor>.toUriList(): List<Uri> =
    mapTo(ArrayList(size)) { descriptor -> descriptor.toUri() }

@Stable
sealed interface MediaBatchState {

    val isActive: Boolean

    val action: Flow<Action>

    suspend fun start(
        media: List<MediaDescriptor>,
        payload: Parcelable? = null,
    )

    suspend fun proceed()

    suspend fun reset()

    sealed interface Action {

        data class Batch(
            val uris: ArrayList<Uri>,
            val ids: LinkedHashSet<Long>,
            val payload: Parcelable?,
        ): Action

        data class Finish(val payload: Parcelable?): Action

        data class Reset(val payload: Parcelable?): Action
    }

    companion object {

        const val BatchSize: Int = 500
    }
}

@Parcelize
data class MediaDescriptor(
    val id: Long,
    val type: Int,
): Parcelable

private class MediaBatchStateImpl: MediaBatchState {

    override var isActive: Boolean by mutableStateOf(false)

    override val action: Flow<MediaBatchState.Action>
        field = MutableSharedFlow()

    override suspend fun start(
        media: List<MediaDescriptor>,
        payload: Parcelable?,
    ) {
        if (media.isEmpty()) {
            return
        }
        isActive = true
        startIndex = 0
        this.media = media
        this.payload = payload
        proceed()
    }

    override suspend fun proceed() {
        if (!isActive) {
            return
        }
        val size = media.size
        if (startIndex >= size) {
            isActive = false
            startIndex = 0
            media = emptyList()
            val payload = this.payload
            this.payload = null
            action.emit(MediaBatchState.Action.Finish(payload))
            return
        }
        val batchStartIndex = startIndex
        val batchEndIndex = (batchStartIndex + MediaBatchState.BatchSize).coerceAtMost(size)
        startIndex = batchEndIndex
        val batch = media.subList(
            fromIndex = batchStartIndex,
            toIndex = batchEndIndex,
        )
        val batchSize = batch.size
        val uris = ArrayList<Uri>(batchSize)
        val ids = newLinkedHashSet<Long>(batchSize)
        for (descriptor in batch) {
            uris.add(descriptor.toUri())
            ids.add(descriptor.id)
        }
        action.emit(
            MediaBatchState.Action.Batch(
                uris = uris,
                ids = ids,
                payload,
            ),
        )
    }

    override suspend fun reset() {
        if (!isActive) {
            return
        }
        isActive = false
        startIndex = 0
        media = emptyList()
        val payload = this.payload
        this.payload = null
        action.emit(MediaBatchState.Action.Reset(payload))
    }

    var startIndex: Int = 0
    var media: List<MediaDescriptor> = emptyList()
    var payload: Parcelable? = null
}

@Parcelize
private data class MediaBatchStateImplConfig(
    val isActive: Boolean,
    val startIndex: Int,
    val media: List<MediaDescriptor>,
    val payload: Parcelable?,
): Parcelable

private object MediaBatchStateImplSaver: Saver<MediaBatchStateImpl, MediaBatchStateImplConfig> {

    override fun SaverScope.save(value: MediaBatchStateImpl): MediaBatchStateImplConfig =
        MediaBatchStateImplConfig(
            isActive = value.isActive,
            startIndex = value.startIndex,
            media = value.media,
            payload = value.payload,
        )

    override fun restore(value: MediaBatchStateImplConfig): MediaBatchStateImpl =
        MediaBatchStateImpl().apply {
            isActive = value.isActive
            startIndex = value.startIndex
            media = value.media
            payload = value.payload
        }
}

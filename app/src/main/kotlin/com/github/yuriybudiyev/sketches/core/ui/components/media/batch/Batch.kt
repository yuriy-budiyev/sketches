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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
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

fun Collection<MediaStoreFile>.toUriList(filterIds: Collection<Long>): List<Uri> {
    val uris = ArrayList<Uri>(this.size.coerceAtMost(filterIds.size))
    for (file in this) {
        if (filterIds.contains(file.id)) {
            uris.add(file.uri)
        }
    }
    return uris
}

fun Collection<MediaStoreFile>.toMediaList(): List<MediaDescriptor> =
    mapTo(ArrayList(size)) { file -> file.toMediaDescriptor() }

fun Collection<MediaStoreFile>.toMediaList(filterIds: Collection<Long>): List<MediaDescriptor> {
    val media = ArrayList<MediaDescriptor>(this.size.coerceAtMost(filterIds.size))
    for (file in this) {
        if (filterIds.contains(file.id)) {
            media.add(file.toMediaDescriptor())
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

    val action: Flow<Action>

    suspend fun start(media: List<MediaDescriptor>) //TODO: Add payload

    suspend fun proceed()

    suspend fun reset()

    sealed interface Action {

        data class Batch(
            val uris: List<Uri>,
            val ids: Set<Long>,
        ): Action

        data object Finish: Action

        data object Reset: Action
    }
}

@Parcelize
data class MediaDescriptor(
    val id: Long,
    val type: Int,
): Parcelable

private class MediaBatchStateImpl: MediaBatchState {

    override val action: Flow<MediaBatchState.Action>
        field = MutableSharedFlow()

    override suspend fun start(media: List<MediaDescriptor>) {
        allMedia = media
        startIndex = 0
        proceed()
    }

    override suspend fun proceed() {
        val size = allMedia.size
        if (size == 0) {
            return
        }
        if (startIndex >= size) {
            startIndex = 0
            allMedia = emptyList()
            action.emit(MediaBatchState.Action.Finish)
            return
        }
        val batchStartIndex = startIndex
        val batchEndIndex = (batchStartIndex + 500).coerceAtMost(size)
        startIndex = batchEndIndex
        val batch = allMedia.subList(
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
            ),
        )
    }

    override suspend fun reset() {
        startIndex = 0
        allMedia = emptyList()
        action.emit(MediaBatchState.Action.Reset)
    }

    var startIndex: Int = 0
    var allMedia: List<MediaDescriptor> = emptyList()
}

@Parcelize
private data class MediaBatchStateImplConfig(
    var startIndex: Int,
    var allMedia: List<MediaDescriptor>,
): Parcelable

private object MediaBatchStateImplSaver: Saver<MediaBatchStateImpl, MediaBatchStateImplConfig> {

    override fun SaverScope.save(value: MediaBatchStateImpl): MediaBatchStateImplConfig =
        MediaBatchStateImplConfig(
            startIndex = value.startIndex,
            allMedia = value.allMedia,
        )

    override fun restore(value: MediaBatchStateImplConfig): MediaBatchStateImpl =
        MediaBatchStateImpl().apply {
            startIndex = value.startIndex
            allMedia = value.allMedia
        }
}

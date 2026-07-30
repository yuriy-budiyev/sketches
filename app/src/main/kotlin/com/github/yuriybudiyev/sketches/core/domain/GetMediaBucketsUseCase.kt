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

package com.github.yuriybudiyev.sketches.core.domain

import android.net.Uri
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.data.repository.MediaStoreRepository
import dagger.Reusable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import java.time.LocalDateTime
import javax.inject.Inject

@Reusable
class GetMediaBucketsUseCase @Inject constructor(private val repository: MediaStoreRepository) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<MediaStoreBucket>> =
        repository.getFiles().transformLatest { files ->
            val bucketsInfo = LinkedHashMap<Long, MediaStoreBucketInfo>()
            for (file in files) {
                val bucketId = file.bucketId
                val coverUri = file.uri
                val coverDateAdded = file.dateAdded
                val bucketInfo = bucketsInfo.getOrPut(bucketId) {
                    MediaStoreBucketInfo(
                        id = bucketId,
                        name = file.bucketName,
                        coverUri = coverUri,
                        coverDateAdded = coverDateAdded,
                        size = 0,
                    )
                }
                bucketInfo.size++
            }
            val buckets = ArrayList<MediaStoreBucket>(bucketsInfo.size)
            for ((_, bucketInfo) in bucketsInfo) {
                buckets.add(
                    MediaStoreBucket(
                        id = bucketInfo.id,
                        name = bucketInfo.name,
                        size = bucketInfo.size,
                        coverUri = bucketInfo.coverUri,
                        coverDateAdded = bucketInfo.coverDateAdded,
                    ),
                )
            }
            emit(buckets)
        }

    private data class MediaStoreBucketInfo(
        val id: Long,
        val name: String,
        val coverUri: Uri,
        val coverDateAdded: LocalDateTime,
        var size: Int,
    )
}

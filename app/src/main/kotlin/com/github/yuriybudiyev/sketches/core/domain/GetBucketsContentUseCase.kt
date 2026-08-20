/*
 * MIT License
 *
 * Copyright (c) 2025 Yuriy Budiyev
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

import com.github.yuriybudiyev.sketches.core.collections.newLinkedHashSet
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatcher
import com.github.yuriybudiyev.sketches.core.coroutines.di.Dispatchers
import com.github.yuriybudiyev.sketches.core.data.model.MediaBucket
import com.github.yuriybudiyev.sketches.core.data.model.MediaFile
import com.github.yuriybudiyev.sketches.core.data.repository.MediaRepository
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Reusable
class GetBucketsContentUseCase @Inject constructor(
    private val repository: MediaRepository,
    @Dispatcher(Dispatchers.Default)
    private val defaultDispatcher: CoroutineDispatcher,
) {

    suspend operator fun invoke(buckets: Collection<MediaBucket>): List<MediaFile> =
        withContext(defaultDispatcher) {
            val allFiles = repository.getFiles().first()
            val bucketIds = newLinkedHashSet<Long>(buckets.size)
            var contentSize = 0
            for (bucket in buckets) {
                bucketIds.add(bucket.id)
                contentSize += bucket.size
            }
            val contentFiles = ArrayList<MediaFile>(contentSize)
            for (file in allFiles) {
                if (bucketIds.contains(file.bucketId)) {
                    contentFiles.add(file)
                }
            }
            return@withContext contentFiles
        }
}

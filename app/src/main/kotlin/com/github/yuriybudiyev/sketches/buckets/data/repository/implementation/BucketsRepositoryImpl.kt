/*
 * MIT License
 *
 * Copyright (c) 2023 Yuriy Budiyev
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

package com.github.yuriybudiyev.sketches.buckets.data.repository.implementation

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.github.yuriybudiyev.sketches.buckets.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.buckets.data.repository.BucketsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BucketsRepositoryImpl(private val context: Context): BucketsRepository {

    override suspend fun getBuckets(): List<MediaStoreBucket>? {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            context
                .getExternalFilesDir(null)
                ?.toUri()
        } ?: return null
        val cursor = withContext(Dispatchers.IO) {
            context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                ),
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )
        } ?: return null
        val bucketCounters = LinkedHashMap<Long, BucketInfo>()
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        while (cursor.moveToNext()) {
            val bucketId = cursor.getLong(bucketIdColumn)
            val bucketName = cursor.getString(bucketNameColumn)
            bucketCounters.getOrPut(bucketId) {
                BucketInfo(
                    bucketId,
                    bucketName,
                    0
                )
            }.imagesCount++
        }
        val buckets = ArrayList<MediaStoreBucket>(bucketCounters.size)
        bucketCounters.forEach { (_, info) ->
            buckets += MediaStoreBucket(
                info.id,
                info.name,
                info.imagesCount
            )
        }
        return buckets
    }

    private class BucketInfo(
        val id: Long,
        val name: String,
        var imagesCount: Int
    ) {

        override fun equals(other: Any?): Boolean =
            when {
                other === this -> true
                other is BucketInfo -> id == other.id
                else -> false
            }

        override fun hashCode(): Int =
            id.hashCode()
    }
}

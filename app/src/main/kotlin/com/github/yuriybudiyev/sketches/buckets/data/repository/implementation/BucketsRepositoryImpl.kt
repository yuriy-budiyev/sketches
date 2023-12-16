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

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.github.yuriybudiyev.sketches.buckets.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.buckets.data.repository.BucketsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BucketsRepositoryImpl(private val context: Context): BucketsRepository {

    override suspend fun getBuckets(): List<MediaStoreBucket>? {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } ?: return null
        val cursor = withContext(Dispatchers.IO) {
            context.contentResolver.query(
                uri,
                arrayOf(
                    MediaStore.Images.Media._ID,
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
        val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val dateAddedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
        while (cursor.moveToNext()) {
            val bucketId = cursor.getLong(bucketIdColumn)
            bucketCounters.getOrPut(bucketId) {
                BucketInfo(
                    id = bucketId,
                    name = cursor.getString(bucketNameColumn),
                    coverUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idColumn)
                    ),
                    coverDate = cursor.getLong(dateAddedColumn) * 1000L,
                    imagesCount = 0
                )
            }.imagesCount++
        }
        val buckets = ArrayList<MediaStoreBucket>(bucketCounters.size)
        bucketCounters.forEach { (_, info) ->
            buckets += MediaStoreBucket(
                id = info.id,
                name = info.name,
                size = info.imagesCount,
                coverUri = info.coverUri,
                coverDate = info.coverDate
            )
        }
        return buckets
    }

    private data class BucketInfo(
        val id: Long,
        val name: String,
        val coverUri: Uri,
        val coverDate: Long,
        var imagesCount: Int
    )
}

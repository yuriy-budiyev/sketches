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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.github.yuriybudiyev.sketches.buckets.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.buckets.data.repository.BucketsRepository

class BucketsRepositoryImpl(private val context: Context): BucketsRepository {

    override suspend fun getBuckets(): List<MediaStoreBucket> {
        val imagesUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val videoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val bucketsInfo = LinkedHashMap<Long, BucketInfo>()
        getBucketsInfoTo(
            imagesUri,
            bucketsInfo
        )
        getBucketsInfoTo(
            videoUri,
            bucketsInfo
        )
        val buckets = ArrayList<MediaStoreBucket>(bucketsInfo.size)
        bucketsInfo.forEach { (_, info) ->
            buckets += MediaStoreBucket(
                id = info.id,
                name = info.name,
                size = info.imagesCount,
                coverUri = info.coverUri,
                coverDateAdded = info.coverDateAdded
            )
        }
        buckets.sortByDescending { bucket -> bucket.coverDateAdded }
        return buckets
    }

    private suspend fun getBucketsInfoTo(
        contentUri: Uri,
        bucketsInfo: MutableMap<Long, BucketInfo>
    ) {
        val cursor = withContext(Dispatchers.IO) {
            context.contentResolver.query(
                contentUri,
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                ),
                null,
                null,
                null
            )
        } ?: return
        val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val dateAddedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val bucketId = cursor.getLong(bucketIdColumn)
            val dateAdded = cursor.getLong(dateAddedColumn) * 1000L
            val bucketInfo = bucketsInfo.getOrPut(bucketId) {
                BucketInfo(
                    id = bucketId,
                    name = cursor.getString(bucketNameColumn),
                    coverUri = ContentUris.withAppendedId(
                        contentUri,
                        id
                    ),
                    coverDateAdded = dateAdded,
                    imagesCount = 0
                )
            }
            bucketInfo.imagesCount++
            if (bucketInfo.coverDateAdded < dateAdded) {
                bucketInfo.coverDateAdded = dateAdded
                bucketInfo.coverUri = ContentUris.withAppendedId(
                    contentUri,
                    id
                )
            }
        }
    }

    private class BucketInfo(
        val id: Long,
        val name: String,
        var coverUri: Uri,
        var coverDateAdded: Long,
        var imagesCount: Int
    )
}

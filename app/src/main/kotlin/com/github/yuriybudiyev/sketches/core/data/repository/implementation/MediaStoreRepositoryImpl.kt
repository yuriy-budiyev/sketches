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

package com.github.yuriybudiyev.sketches.core.data.repository.implementation

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.collection.MutableLongObjectMap
import androidx.core.database.getStringOrNull
import androidx.core.net.toUri
import com.github.yuriybudiyev.sketches.core.constants.SketchesConstants
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.data.repository.MediaStoreRepository
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreRepositoryImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
): MediaStoreRepository {

    private fun collectFiles(
        mediaType: MediaType,
        bucketId: Long,
    ): List<MediaStoreFile> {
        val contentUri = mediaType.contentUri
        val cursor = context.contentResolver.query(
            contentUri,
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.MIME_TYPE,
            ),
            if (bucketId != SketchesConstants.NoId) {
                "${MediaStore.MediaColumns.BUCKET_ID}=?"
            } else {
                null
            },
            if (bucketId != SketchesConstants.NoId) {
                arrayOf(bucketId.toString())
            } else {
                null
            },
            null
        ) ?: return emptyList()
        cursor.use { c ->
            val files = ArrayList<MediaStoreFile>(c.count)
            val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val bucketIdColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                files += MediaStoreFile(
                    id = id,
                    bucketId = c.getLong(bucketIdColumn),
                    dateAdded = c.getLong(dateAddedColumn) * 1000L,
                    mediaType = mediaType,
                    mimeType = c.getStringOrNull(mimeTypeColumn) ?: mediaType.mimeType,
                    uri = ContentUris
                        .withAppendedId(
                            contentUri,
                            id
                        )
                        .toString()
                )
            }
            return files
        }
    }

    override suspend fun getFiles(bucketId: Long): List<MediaStoreFile> {
        val imageFiles = collectFiles(
            MediaType.Image,
            bucketId
        )
        val videoFiles = collectFiles(
            MediaType.Video,
            bucketId
        )
        val files = ArrayList<MediaStoreFile>(imageFiles.size + videoFiles.size)
        files.addAll(imageFiles)
        files.addAll(videoFiles)
        files.sortByDescending { file -> file.dateAdded }
        return files
    }

    override suspend fun deleteFiles(files: Collection<MediaStoreFile>): Boolean {
        val contentResolver = context.contentResolver
        var count = 0
        for (file in files) {
            count += contentResolver.delete(
                file.uri.toUri(),
                null,
                null
            )
        }
        return count > 0
    }

    private fun collectBucketsInfo(
        mediaType: MediaType,
        destination: MutableLongObjectMap<BucketInfo>,
    ) {
        val contentUri = mediaType.contentUri
        val cursor = context.contentResolver.query(
            contentUri,
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED,
            ),
            null,
            null,
            null
        ) ?: return
        cursor.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val bucketIdColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn =
                c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val bucketId = c.getLong(bucketIdColumn)
                val dateAdded = c.getLong(dateAddedColumn) * 1000L
                val bucketInfo = destination.getOrPut(bucketId) {
                    BucketInfo(
                        id = bucketId,
                        name = c.getStringOrNull(bucketNameColumn) ?: id.toString(),
                        coverUri = ContentUris
                            .withAppendedId(
                                contentUri,
                                id
                            )
                            .toString(),
                        coverDateAdded = dateAdded,
                        size = 0
                    )
                }
                bucketInfo.size++
                if (bucketInfo.coverDateAdded < dateAdded) {
                    bucketInfo.coverDateAdded = dateAdded
                    bucketInfo.coverUri = ContentUris
                        .withAppendedId(
                            contentUri,
                            id
                        )
                        .toString()
                }
            }
        }
    }

    override suspend fun getBuckets(): List<MediaStoreBucket> {
        val bucketsInfo = MutableLongObjectMap<BucketInfo>(256)
        collectBucketsInfo(
            MediaType.Image,
            bucketsInfo
        )
        collectBucketsInfo(
            MediaType.Video,
            bucketsInfo
        )
        val buckets = ArrayList<MediaStoreBucket>(bucketsInfo.size)
        bucketsInfo.forEachValue { info ->
            buckets += MediaStoreBucket(
                info.id,
                info.name,
                info.size,
                info.coverUri,
                info.coverDateAdded
            )
        }
        buckets.sortByDescending { bucket -> bucket.coverDateAdded }
        return buckets
    }

    private data class BucketInfo(
        val id: Long,
        val name: String,
        var size: Int,
        var coverUri: String,
        var coverDateAdded: Long,
    )
}

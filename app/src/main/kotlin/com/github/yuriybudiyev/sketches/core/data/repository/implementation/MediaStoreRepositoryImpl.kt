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

package com.github.yuriybudiyev.sketches.core.data.repository.implementation

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.data.model.MediaType
import com.github.yuriybudiyev.sketches.core.data.repository.MediaStoreRepository
import com.github.yuriybudiyev.sketches.core.util.data.contentUriFor

class MediaStoreRepositoryImpl(private val context: Context): MediaStoreRepository {

    private suspend fun collectFiles(
        mediaType: MediaType,
        bucketId: Long,
    ): List<MediaStoreFile> {
        val contentUri = contentUriFor(mediaType)
        val cursor = withContext(Dispatchers.IO) {
            context.contentResolver.query(
                contentUri,
                arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.BUCKET_ID,
                    MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.MediaColumns.DATE_ADDED,
                    MediaStore.MediaColumns.MIME_TYPE,
                ),
                if (bucketId != -1L) {
                    "${MediaStore.MediaColumns.BUCKET_ID}=?"
                } else {
                    null
                },
                if (bucketId != -1L) {
                    arrayOf(bucketId.toString())
                } else {
                    null
                },
                null
            )
        } ?: return emptyList()
        cursor.use { c ->
            val files = ArrayList<MediaStoreFile>(c.count)
            val idColumn = c.getColumnIndex(MediaStore.MediaColumns._ID)
            val nameColumn = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val bucketIdColumn = c.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = c.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val dateAddedColumn = c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val mimeTypeColumn = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                files += MediaStoreFile(
                    id = id,
                    name = c.getString(nameColumn),
                    mediaType = mediaType,
                    bucketId = c.getLong(bucketIdColumn),
                    bucketName = c.getString(bucketNameColumn),
                    dateAdded = c.getLong(dateAddedColumn) * 1000L,
                    mimeType = c.getString(mimeTypeColumn),
                    uri = ContentUris.withAppendedId(
                        contentUri,
                        id
                    )
                )
            }
            return files
        }
    }

    override suspend fun getFiles(bucketId: Long): List<MediaStoreFile> {
        val imageFiles = collectFiles(
            MediaType.IMAGE,
            bucketId
        )
        val videoFiles = collectFiles(
            MediaType.VIDEO,
            bucketId
        )
        val files = ArrayList<MediaStoreFile>(imageFiles.size + videoFiles.size)
        files.addAll(imageFiles)
        files.addAll(videoFiles)
        files.sortByDescending { file -> file.dateAdded }
        return files
    }

    override suspend fun deleteFile(uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            context.contentResolver.delete(
                uri,
                null,
                null
            )
        } > 0

    private suspend fun collectBucketsInfo(
        mediaType: MediaType,
        destination: MutableMap<Long, BucketInfo>,
    ) {
        val contentUri = contentUriFor(mediaType)
        val cursor = withContext(Dispatchers.IO) {
            context.contentResolver.query(
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
            )
        } ?: return
        cursor.use { c ->
            val idColumn = c.getColumnIndex(MediaStore.MediaColumns._ID)
            val bucketIdColumn = c.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = c.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val dateAddedColumn = c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val bucketId = c.getLong(bucketIdColumn)
                val dateAdded = c.getLong(dateAddedColumn) * 1000L
                val bucketInfo = destination.getOrPut(bucketId) {
                    BucketInfo(
                        id = bucketId,
                        name = c.getString(bucketNameColumn),
                        coverUri = ContentUris.withAppendedId(
                            contentUri,
                            id
                        ),
                        coverDateAdded = dateAdded,
                        size = 0
                    )
                }
                bucketInfo.size++
                if (bucketInfo.coverDateAdded < dateAdded) {
                    bucketInfo.coverDateAdded = dateAdded
                    bucketInfo.coverUri = ContentUris.withAppendedId(
                        contentUri,
                        id
                    )
                }
            }
        }
    }

    override suspend fun getBuckets(): List<MediaStoreBucket> {
        val bucketsInfo = LinkedHashMap<Long, BucketInfo>()
        collectBucketsInfo(
            MediaType.IMAGE,
            bucketsInfo
        )
        collectBucketsInfo(
            MediaType.VIDEO,
            bucketsInfo
        )
        return bucketsInfo
            .mapTo(ArrayList(bucketsInfo.size)) { (_, info) -> info.toBucket() }
            .also { buckets -> buckets.sortByDescending { bucket -> bucket.coverDateAdded } }
    }

    private fun BucketInfo.toBucket(): MediaStoreBucket =
        MediaStoreBucket(
            id,
            name,
            size,
            coverUri,
            coverDateAdded
        )

    private data class BucketInfo(
        val id: Long,
        val name: String,
        var size: Int,
        var coverUri: Uri,
        var coverDateAdded: Long,
    )
}

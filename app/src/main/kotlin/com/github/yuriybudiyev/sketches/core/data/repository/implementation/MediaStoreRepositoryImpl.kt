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
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.data.model.MediaType
import com.github.yuriybudiyev.sketches.core.data.repository.MediaStoreRepository

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
        val files = ArrayList<MediaStoreFile>(cursor.count)
        val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        val dateAddedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
        val mimeTypeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            files += MediaStoreFile(
                id = id,
                name = cursor.getString(nameColumn),
                mediaType = mediaType,
                bucketId = cursor.getLong(bucketIdColumn),
                bucketName = cursor.getString(bucketNameColumn),
                dateAdded = cursor.getLong(dateAddedColumn) * 1000L,
                mimeType = cursor.getString(mimeTypeColumn),
                uri = ContentUris.withAppendedId(
                    contentUri,
                    id
                )
            )
        }
        return files
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
            ) > 0
        }

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
        val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        val dateAddedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val bucketId = cursor.getLong(bucketIdColumn)
            val dateAdded = cursor.getLong(dateAddedColumn) * 1000L
            val bucketInfo = destination.getOrPut(bucketId) {
                BucketInfo(
                    id = bucketId,
                    name = cursor.getString(bucketNameColumn),
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

    private fun contentUriFor(mediaType: MediaType): Uri =
        when (mediaType) {
            MediaType.IMAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            }
            MediaType.VIDEO -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            }
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

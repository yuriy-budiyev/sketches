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
import android.net.Uri
import android.provider.MediaStore
import androidx.collection.MutableLongObjectMap
import androidx.core.database.getStringOrNull
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreBucket
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.data.repository.MediaStoreRepository
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreRepositoryImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
): MediaStoreRepository {

    private fun collectFiles(
        mediaType: MediaType,
        bucketId: Long?,
    ): List<MediaStoreFile> {
        val contentUri = mediaType.contentUri
        val cursor = context.contentResolver.query(
            contentUri,
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.BUCKET_ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.MIME_TYPE,
            ),
            if (bucketId != null) {
                "${MediaStore.MediaColumns.BUCKET_ID}=?"
            } else {
                null
            },
            if (bucketId != null) {
                arrayOf(bucketId.toString())
            } else {
                null
            },
            null,
        ) ?: return emptyList()
        cursor.use { c ->
            val files = ArrayList<MediaStoreFile>(c.count)
            val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val bucketIdColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val displayNameColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                files.add(
                    MediaStoreFile(
                        id = id,
                        bucketId = c.getLong(bucketIdColumn),
                        displayName = c.getStringOrNull(displayNameColumn) ?: id.toString(),
                        dateAdded = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(c.getLong(dateAddedColumn)),
                            ZoneId.systemDefault(),
                        ),
                        mediaType = mediaType,
                        mimeType = c.getStringOrNull(mimeTypeColumn) ?: mediaType.mimeType,
                        uri = ContentUris
                            .withAppendedId(
                                contentUri,
                                id,
                            ),
                    ),
                )
            }
            return files
        }
    }

    override suspend fun deleteContent(uris: Collection<Uri>): Boolean {
        val contentResolver = context.contentResolver
        var count = 0
        for (uri in uris) {
            count += contentResolver.delete(
                uri,
                null,
                null,
            )
        }
        return count == uris.size
    }

    override suspend fun getFiles(bucketId: Long?): List<MediaStoreFile> {
        val imageFiles = collectFiles(
            MediaType.Image,
            bucketId,
        )
        val videoFiles = collectFiles(
            MediaType.Video,
            bucketId,
        )
        val filesCount = imageFiles.size + videoFiles.size
        if (filesCount == 0) {
            return emptyList()
        }
        val files = ArrayList<MediaStoreFile>(filesCount)
        files.addAll(imageFiles)
        files.addAll(videoFiles)
        files.sortByDescending { file -> file.dateAdded }
        return files
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
            null,
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
                val dateAdded = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(c.getLong(dateAddedColumn)),
                    ZoneId.systemDefault(),
                )
                val bucketInfo = destination.getOrPut(bucketId) {
                    BucketInfo(
                        id = bucketId,
                        name = c.getStringOrNull(bucketNameColumn) ?: id.toString(),
                        coverUri = ContentUris
                            .withAppendedId(
                                contentUri,
                                id,
                            ),
                        coverDateAdded = dateAdded,
                        size = 0,
                    )
                }
                bucketInfo.size++
                if (bucketInfo.coverDateAdded < dateAdded) {
                    bucketInfo.coverDateAdded = dateAdded
                    bucketInfo.coverUri = ContentUris
                        .withAppendedId(
                            contentUri,
                            id,
                        )
                }
            }
        }
    }

    override suspend fun getBuckets(): List<MediaStoreBucket> {
        val bucketsInfo = MutableLongObjectMap<BucketInfo>(128)
        collectBucketsInfo(
            mediaType = MediaType.Image,
            destination = bucketsInfo,
        )
        collectBucketsInfo(
            mediaType = MediaType.Video,
            destination = bucketsInfo,
        )
        val buckets = ArrayList<MediaStoreBucket>(bucketsInfo.size)
        bucketsInfo.forEachValue { info ->
            buckets.add(
                MediaStoreBucket(
                    id = info.id,
                    name = info.name,
                    size = info.size,
                    coverUri = info.coverUri,
                    coverDateAdded = info.coverDateAdded,
                ),
            )
        }
        buckets.sortByDescending { bucket -> bucket.coverDateAdded }
        return buckets
    }

    private data class BucketInfo(
        val id: Long,
        val name: String,
        var size: Int,
        var coverUri: Uri,
        var coverDateAdded: LocalDateTime,
    )
}

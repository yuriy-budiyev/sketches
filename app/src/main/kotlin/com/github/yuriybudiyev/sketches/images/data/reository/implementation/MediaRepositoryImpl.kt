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

package com.github.yuriybudiyev.sketches.images.data.reository.implementation

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.github.yuriybudiyev.sketches.images.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.images.data.reository.MediaRepository

class MediaRepositoryImpl(private val context: Context): MediaRepository {

    override suspend fun getMedia(bucketId: Long): List<MediaStoreFile> {
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
        val images = getMedia(
            imagesUri,
            MediaStoreFile.Type.IMAGE,
            bucketId
        )
        val video = getMedia(
            videoUri,
            MediaStoreFile.Type.VIDEO,
            bucketId
        )
        val media = ArrayList<MediaStoreFile>(images.size + video.size)
        media.addAll(images)
        media.addAll(video)
        media.sortByDescending { file -> file.dateAdded }
        return media
    }

    private suspend fun getMedia(
        contentUri: Uri,
        mediaType: MediaStoreFile.Type,
        bucketId: Long,
    ): List<MediaStoreFile> {
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
                type = mediaType,
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
}

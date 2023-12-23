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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.github.yuriybudiyev.sketches.images.data.model.MediaStoreImage
import com.github.yuriybudiyev.sketches.images.data.reository.ImagesRepository

class ImagesRepositoryImpl(private val context: Context): ImagesRepository {

    override suspend fun getImages(bucketId: Long): List<MediaStoreImage>? {
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
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.WIDTH,
                    MediaStore.Images.Media.HEIGHT,
                    MediaStore.Images.Media.ORIENTATION,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.Media.SIZE
                ),
                if (bucketId != -1L) {
                    "${MediaStore.Images.Media.BUCKET_ID}=?"
                } else {
                    null
                },
                if (bucketId != -1L) {
                    arrayOf(bucketId.toString())
                } else {
                    null
                },
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )
        } ?: return null
        val images = ArrayList<MediaStoreImage>(cursor.count)
        val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val dateAddedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
        val withColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
        val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
        val orientationColumn = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION)
        val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
        val sizeColumn = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            images += MediaStoreImage(
                id = id,
                name = cursor.getString(nameColumn),
                bucketId = cursor.getLong(bucketIdColumn),
                bucketName = cursor.getString(bucketNameColumn),
                dateAdded = cursor.getLong(dateAddedColumn) * 1000L,
                width = cursor.getInt(withColumn),
                height = cursor.getInt(heightColumn),
                orientation = cursor.getInt(orientationColumn),
                mimeType = cursor.getString(mimeTypeColumn),
                sizeBytes = cursor.getLong(sizeColumn),
                uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
            )
        }
        return images
    }
}

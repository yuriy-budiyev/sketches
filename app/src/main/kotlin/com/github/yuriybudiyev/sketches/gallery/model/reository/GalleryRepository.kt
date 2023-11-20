package com.github.yuriybudiyev.sketches.gallery.model.reository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.github.yuriybudiyev.sketches.gallery.model.data.GalleryImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleryRepository(private val context: Context) {

    suspend fun getMediaStoreImages(): List<GalleryImage>? {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            context.getExternalFilesDir(null)?.toUri()
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
                    MediaStore.Images.Media.HEIGHT

                ),
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )
        } ?: return null
        val images = ArrayList<GalleryImage>(cursor.count)
        val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val dateAddedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
        val withColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
        val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            images += GalleryImage(
                id,
                cursor.getString(nameColumn),
                cursor.getLong(bucketIdColumn),
                cursor.getString(bucketNameColumn),
                cursor.getLong(dateAddedColumn) * 1000L,
                cursor.getInt(withColumn),
                cursor.getInt(heightColumn),
                ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
            )
        }
        return images
    }
}

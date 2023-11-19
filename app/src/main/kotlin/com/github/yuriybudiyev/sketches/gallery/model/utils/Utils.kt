package com.github.yuriybudiyev.sketches.gallery.model.utils

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.github.yuriybudiyev.sketches.gallery.model.data.MediaStoreImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun Context.getMediaStoreImages(): List<MediaStoreImage>? {
    val context = applicationContext
    val mediaStoreUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        context.getExternalFilesDir(null)?.toUri()
    } ?: return null
    val cursor = withContext(Dispatchers.IO) {
        context.contentResolver.query(
            mediaStoreUri,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA
            ),
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
    } ?: return null
    val images = ArrayList<MediaStoreImage>(cursor.count)

    return images
}


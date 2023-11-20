package com.github.yuriybudiyev.sketches.gallery.model.data

import android.net.Uri

data class GalleryImage(
    val id: Long,
    val name: String,
    val bucketId: Long,
    val bucketName: String,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
    val uri: Uri
)

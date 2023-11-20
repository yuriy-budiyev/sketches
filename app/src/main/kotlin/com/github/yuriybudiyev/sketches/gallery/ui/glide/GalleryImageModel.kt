package com.github.yuriybudiyev.sketches.gallery.ui.glide

import com.bumptech.glide.load.Key
import com.github.yuriybudiyev.sketches.gallery.model.data.GalleryImage
import java.security.MessageDigest

class GalleryImageModel(val image: GalleryImage) : Key {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(keyBytes)
    }

    override fun equals(other: Any?): Boolean =
        when {
            other === this -> true
            other is GalleryImageModel -> other.key == key
            else -> false
        }

    override fun hashCode(): Int =
        key.hashCode()

    override fun toString(): String =
        key

    private val key: String =
        "galley_image_${image.id}"
    private val keyBytes: ByteArray =
        key.toByteArray(Key.CHARSET)
}

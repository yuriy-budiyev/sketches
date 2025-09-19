package com.github.yuriybudiyev.sketches.core.platform.share

import android.net.Uri
import androidx.core.net.toUri
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import java.util.EnumSet

data class ShareInfo(
    val uris: ArrayList<Uri>,
    val mimeType: String,
)

fun Collection<MediaStoreFile>.toShareInfo(): ShareInfo {
    val size = this.size
    val uris = ArrayList<Uri>(size)
    if (size == 1) {
        val file = this.first()
        uris.add(file.uri.toUri())
        return ShareInfo(
            uris = uris,
            mimeType = file.mimeType,
        )
    }
    val mediaTypes = EnumSet.noneOf(MediaType::class.java)
    for (file in this) {
        uris.add(file.uri.toUri())
        mediaTypes.add(file.mediaType)
    }
    return ShareInfo(
        uris = uris,
        mimeType = if (mediaTypes.size == 1) {
            when (mediaTypes.first()) {
                MediaType.Image -> "image/*"
                MediaType.Video -> "video/*"
            }
        } else {
            "*/*"
        },
    )
}

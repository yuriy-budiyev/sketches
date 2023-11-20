package com.github.yuriybudiyev.sketches.core.glide

import android.net.Uri
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.github.yuriybudiyev.sketches.gallery.model.data.GalleryImage

class GalleryImageLoader : ModelLoader<GalleryImage, Uri> {
    override fun buildLoadData(
        model: GalleryImage,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<Uri> =
        ModelLoader.LoadData(
            GalleryImageKey(model),
            GalleryImageFetcher(model)
        )

    override fun handles(model: GalleryImage): Boolean =
        true
}

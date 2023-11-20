package com.github.yuriybudiyev.sketches.gallery.model.glide

import android.content.Context
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.github.yuriybudiyev.sketches.gallery.model.data.GalleryImage
import java.io.InputStream

class GalleryImageLoader(private val context: Context) : ModelLoader<GalleryImage, InputStream> {
    override fun buildLoadData(
        model: GalleryImage,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream> =
        ModelLoader.LoadData(
            GalleryImageKey(model),
            GalleryImageFetcher(
                context,
                model
            )
        )

    override fun handles(model: GalleryImage): Boolean =
        true
}

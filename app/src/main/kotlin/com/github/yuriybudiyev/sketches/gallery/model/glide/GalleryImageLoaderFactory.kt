package com.github.yuriybudiyev.sketches.gallery.model.glide

import android.content.Context
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.github.yuriybudiyev.sketches.gallery.model.data.GalleryImage
import java.io.InputStream

class GalleryImageLoaderFactory(private val context: Context) :
    ModelLoaderFactory<GalleryImage, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GalleryImage, InputStream> =
        GalleryImageLoader(context)

    override fun teardown() {
    }
}

package com.github.yuriybudiyev.sketches.gallery.model.glide

import android.net.Uri
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.github.yuriybudiyev.sketches.gallery.model.data.GalleryImage

class GalleryImageLoaderFactory : ModelLoaderFactory<GalleryImage, Uri> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<GalleryImage, Uri> =
        GalleryImageLoader()

    override fun teardown() {
    }
}

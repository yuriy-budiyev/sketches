package com.github.yuriybudiyev.sketches.gallery.model.glide

import android.net.Uri
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.github.yuriybudiyev.sketches.gallery.model.data.GalleryImage

class GalleryImageFetcher(private val image: GalleryImage) : DataFetcher<Uri> {
    override fun loadData(
        priority: Priority,
        callback: DataFetcher.DataCallback<in Uri>
    ) {
        callback.onDataReady(image.uri)
    }

    override fun cleanup() {
    }

    override fun cancel() {
    }

    override fun getDataClass(): Class<Uri> =
        Uri::class.java

    override fun getDataSource(): DataSource =
        DataSource.LOCAL
}

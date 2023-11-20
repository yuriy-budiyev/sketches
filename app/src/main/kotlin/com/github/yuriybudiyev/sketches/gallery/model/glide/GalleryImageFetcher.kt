package com.github.yuriybudiyev.sketches.gallery.model.glide

import android.content.Context
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.github.yuriybudiyev.sketches.gallery.model.data.GalleryImage
import okhttp3.internal.closeQuietly
import java.io.InputStream

class GalleryImageFetcher(
    private val context: Context,
    private val image: GalleryImage
) : DataFetcher<InputStream> {

    override fun loadData(
        priority: Priority,
        callback: DataFetcher.DataCallback<in InputStream>
    ) {
        try {
            val stream = context.contentResolver.openInputStream(image.uri)
            if (stream != null) {
                callback.onDataReady(stream)
            } else {
                callback.onLoadFailed(IllegalStateException("Content resolver returned null"))
            }
            this.stream = stream
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        }
    }

    override fun cleanup() {
        stream?.closeQuietly()
        stream = null
    }

    override fun cancel() {
        stream?.closeQuietly()
        stream = null
    }

    override fun getDataClass(): Class<InputStream> =
        InputStream::class.java

    override fun getDataSource(): DataSource =
        DataSource.LOCAL

    private var stream: InputStream? = null
}

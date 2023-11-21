/*
 * MIT License
 *
 * Copyright (c) 2023 Yuriy Budiyev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

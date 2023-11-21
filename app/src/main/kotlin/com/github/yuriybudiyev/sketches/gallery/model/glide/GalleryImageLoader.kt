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
 *
 */

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

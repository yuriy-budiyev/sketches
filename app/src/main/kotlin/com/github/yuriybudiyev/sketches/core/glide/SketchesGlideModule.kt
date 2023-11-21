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

package com.github.yuriybudiyev.sketches.core.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.executor.GlideExecutor
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableDecoder
import com.bumptech.glide.module.AppGlideModule
import com.github.yuriybudiyev.sketches.gallery.model.data.GalleryImage
import com.github.yuriybudiyev.sketches.gallery.model.glide.GalleryImageLoaderFactory
import java.io.InputStream
import kotlin.math.min

@GlideModule(glideName = "SketchesGlide")
class SketchesGlideModule : AppGlideModule() {

    override fun registerComponents(
        context: Context,
        glide: Glide,
        registry: Registry
    ) {
        registry.append(
            GalleryImage::class.java,
            InputStream::class.java,
            GalleryImageLoaderFactory(context)
        )
        val svgBitmapDecoder = SvgBitmapDecoder(glide.bitmapPool)
        registry.append(
            Registry.BUCKET_BITMAP,
            InputStream::class.java,
            Bitmap::class.java,
            svgBitmapDecoder
        )
        registry.append(
            Registry.BUCKET_BITMAP_DRAWABLE,
            InputStream::class.java,
            BitmapDrawable::class.java,
            BitmapDrawableDecoder(
                context.resources,
                svgBitmapDecoder
            )
        )
    }

    override fun applyOptions(
        context: Context,
        builder: GlideBuilder
    ) {
        val threadPoolSize = min(
            Runtime.getRuntime().availableProcessors(),
            4
        )
        val sourceExecutor = GlideExecutor.newSourceBuilder()
            .setThreadCount(threadPoolSize)
            .setName("glide-source")
            .setUncaughtThrowableStrategy(GlideExecutor.UncaughtThrowableStrategy.DEFAULT)
            .build()
        builder.setSourceExecutor(sourceExecutor)
        val diskCacheExecutor = GlideExecutor.newDiskCacheBuilder()
            .setThreadCount(threadPoolSize)
            .setName("glide-disk-cache")
            .setUncaughtThrowableStrategy(GlideExecutor.UncaughtThrowableStrategy.DEFAULT)
            .build()
        builder.setDiskCacheExecutor(diskCacheExecutor)
        val animationExecutor = GlideExecutor.newAnimationBuilder()
            .setThreadCount(threadPoolSize)
            .setName("glide-animation")
            .setUncaughtThrowableStrategy(GlideExecutor.UncaughtThrowableStrategy.DEFAULT)
            .build()
        builder.setAnimationExecutor(animationExecutor)
    }
}

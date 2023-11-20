package com.github.yuriybudiyev.sketches.core.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.executor.GlideExecutor
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

/*
 * MIT License
 *
 * Copyright (c) 2026 Yuriy Budiyev
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

package com.github.yuriybudiyev.sketches.core.imageloader.cache

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.collection.LruCache
import androidx.compose.runtime.Immutable
import coil3.BitmapImage
import coil3.Extras
import coil3.Image
import coil3.asImage
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Dimension
import coil3.target.ViewTarget
import coil3.toBitmap
import com.github.yuriybudiyev.sketches.core.math.toIntClamped
import com.github.yuriybudiyev.sketches.core.platform.memory.getMaxMemory

fun ImageRequest.Builder.allowLocalCacheIntercept(allow: Boolean): ImageRequest.Builder {
    extras[AllowLocalCacheInterceptKey] = allow
    return this
}

private val AllowLocalCacheInterceptKey: Extras.Key<Boolean> = Extras.Key(default = false)

class LocalCacheInterceptor(
    private val memoryCache: ImageMemoryCache,
    private val diskCache: DiskCache,
): Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        if (request.extras[AllowLocalCacheInterceptKey] != true) {
            return chain.proceed()
        }
        val uri = request.data as? String ?: return chain.proceed()
        val colonIndex = uri.indexOf(':')
        if (colonIndex < 1) {
            return chain.proceed()
        }
        val scheme = uri.substring(startIndex = 0, endIndex = colonIndex)
        if (scheme != "content" && scheme != "file") {
            return chain.proceed()
        }
        val size = chain.size
        val width = (size.width as? Dimension.Pixels)?.px ?: return chain.proceed()
        val height = (size.height as? Dimension.Pixels)?.px ?: return chain.proceed()
        val imageKey = ImageKey(uri, width, height)
        val hardwareAllowed = request.allowHardware && request.target.let { target ->
            target !is ViewTarget<*> || target.view.isHardwareAccelerated
        }
        val memoryImage = memoryCache[imageKey]
        if (
            memoryImage != null && memoryImage.let { memoryImage ->
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O || memoryImage !is BitmapImage
                    || memoryImage.bitmap.config != Bitmap.Config.HARDWARE || hardwareAllowed
            }
        ) {
            return SuccessResult(
                image = memoryImage,
                request = request,
                dataSource = DataSource.MEMORY_CACHE,
                memoryCacheKey = null,
                diskCacheKey = null,
                isSampled = false,
                isPlaceholderCached = false,
            )
        }
        diskCache.openSnapshot(imageKey.toString())?.use { snapshot ->
            val bitmap = diskCache.fileSystem.read(snapshot.data) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(peek().inputStream(), null, options)
                options.inJustDecodeBounds = false
                options.inMutable = false
                options.inPreferredConfig =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (hardwareAllowed) {
                            Bitmap.Config.HARDWARE
                        } else {
                            if (options.outConfig == Bitmap.Config.RGBA_F16) {
                                Bitmap.Config.RGBA_F16
                            } else {
                                Bitmap.Config.ARGB_8888
                            }
                        }
                    } else {
                        Bitmap.Config.ARGB_8888
                    }
                BitmapFactory.decodeStream(inputStream(), null, options)
            }
            if (bitmap != null) {
                val diskImage = bitmap.asImage(shareable = !bitmap.isMutable)
                memoryCache[imageKey] = diskImage
                return SuccessResult(
                    image = diskImage,
                    request = request,
                    dataSource = DataSource.DISK,
                    memoryCacheKey = null,
                    diskCacheKey = null,
                    isSampled = false,
                    isPlaceholderCached = false,
                )
            }
        }
        val result = chain.proceed()
        if (result is SuccessResult) {
            val bitmap = result.image.toBitmap()
            diskCache.openEditor(imageKey.toString())?.let { editor ->
                diskCache.fileSystem.write(editor.metadata) {
                    writeUtf8("${imageKey.uri}\n")
                    writeUtf8("${imageKey.width}\n")
                    writeUtf8("${imageKey.height}\n")
                }
                diskCache.fileSystem.write(editor.data) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 95, outputStream())
                    } else {
                        @Suppress("DEPRECATION")
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 95, outputStream())
                    }
                }
                editor.commit()
            }
            val resultImage = bitmap.asImage(shareable = true)
            memoryCache[imageKey] = resultImage
            return SuccessResult(
                image = resultImage,
                request = request,
                dataSource = DataSource.DISK,
                memoryCacheKey = null,
                diskCacheKey = null,
                isSampled = false,
                isPlaceholderCached = false,
            )
        }
        return result
    }
}

inline val Context.imageMemoryCache: ImageMemoryCache
    get() = ImageMemoryCache.instance(this)

class ImageMemoryCache private constructor(private val maxSizeBytes: Long) {

    operator fun set(
        key: ImageKey,
        image: Image,
    ) {
        if (image.size >= imageCache.maxSize()) {
            return
        }
        imageCache[key] = image
        synchronized(keyCache) {
            val oldKey = keyCache[key.uri]
            if (oldKey == null || key.width * key.height > oldKey.width * oldKey.height) {
                keyCache[key.uri] = key
            }
        }
    }

    operator fun get(uri: String): Image? =
        imageCache[synchronized(keyCache) { keyCache[uri] } ?: return null]

    operator fun get(key: ImageKey): Image? =
        imageCache[key]

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun LruCache<ImageKey, Image>.set(
        key: ImageKey,
        image: Image,
    ) {
        put(key, image)
    }

    private val imageCache: LruCache<ImageKey, Image> = CacheImpl()
    private val keyCache: MutableMap<String, ImageKey> = LinkedHashMap()
    private val memoryCallbacks: ComponentCallbacks2 = CallbacksImpl()

    private inner class CallbacksImpl: ComponentCallbacks2 {

        override fun onTrimMemory(level: Int) {
            when {
                level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                    imageCache.evictAll()
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                    imageCache.trimToSize(imageCache.size() / 2)
                }
            }
        }

        override fun onConfigurationChanged(newConfig: Configuration) {}

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onLowMemory() {
            onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        }
    }

    private inner class CacheImpl: LruCache<ImageKey, Image>(maxSizeBytes.toIntClamped()) {

        override fun sizeOf(
            key: ImageKey,
            value: Image,
        ): Int =
            value.size.toIntClamped()

        override fun entryRemoved(
            evicted: Boolean,
            key: ImageKey,
            oldValue: Image,
            newValue: Image?,
        ) {
            synchronized(keyCache) {
                keyCache.remove(
                    key.uri,
                    key,
                )
            }
        }
    }

    companion object {

        fun instance(context: Context): ImageMemoryCache {
            var instance = this.instance
            if (instance !== null) {
                return instance
            }
            synchronized(this) {
                instance = this.instance
                if (instance === null) {
                    val appContext = context.applicationContext
                    instance = ImageMemoryCache(appContext.getMaxMemory() / 4L)
                    appContext.registerComponentCallbacks(instance.memoryCallbacks)
                    this.instance = instance
                }
                return instance
            }
        }

        @Volatile
        private var instance: ImageMemoryCache? = null
    }
}

@Immutable
class ImageKey(
    val uri: String,
    val width: Int,
    val height: Int,
) {

    override fun equals(other: Any?): Boolean =
        when {
            other === this -> true
            other is ImageKey ->
                other.uri == this.uri
                    && other.width == this.width
                    && other.height == this.height
            else -> false
        }

    private val cachedHashCode: Int = run {
        var result = 17
        result = 31 * result + uri.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        return@run result
    }

    override fun hashCode(): Int =
        cachedHashCode

    private val cachedString: String =
        "$uri/$width/$height"

    override fun toString(): String =
        cachedString
}

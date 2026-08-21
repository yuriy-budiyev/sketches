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

package com.github.yuriybudiyev.sketches.core.coil

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.collection.LruCache
import coil3.Extras
import coil3.Image
import coil3.asImage
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.toBitmap
import com.github.yuriybudiyev.sketches.core.platform.memory.getMaxMemory

fun ImageRequest.Builder.allowLocalCacheIntercept(allow: Boolean): ImageRequest.Builder {
    extras[AllowLocalCacheInterceptKey] = allow
    return this
}

private val AllowLocalCacheInterceptKey: Extras.Key<Boolean> = Extras.Key(default = false)

class LocalCacheInterceptor(
    private val memoryCache: LruMemoryCache,
    private val diskCache: DiskCache,
): Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        if (request.extras[AllowLocalCacheInterceptKey] != true) {
            return chain.proceed()
        }
        val uri = request.data as? Uri ?: return chain.proceed()
        val uriScheme = uri.scheme
        if (uriScheme != "content" && uriScheme != "file") {
            return chain.proceed()
        }
        val size = chain.size
        val width = (size.width as? Dimension.Pixels)?.px ?: return chain.proceed()
        val height = (size.height as? Dimension.Pixels)?.px ?: return chain.proceed()
        val uriString = uri.toString()
        val memoryCacheKey = LruMemoryCache.Key(
            uri = uriString,
            width = width,
            height = height,
        )
        val diskCacheKey = "$uriString/$width/$height"
        val memoryImage = memoryCache[memoryCacheKey]
        if (memoryImage != null) {
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
        diskCache.openSnapshot(diskCacheKey)?.use { snapshot ->
            val bitmap = diskCache.fileSystem.read(snapshot.data) {
                BitmapFactory.decodeStream(inputStream())
            }
            if (bitmap != null) {
                val diskImage = bitmap.asImage(shareable = true)
                memoryCache[memoryCacheKey] = diskImage
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
            diskCache.openEditor(diskCacheKey)?.let { editor ->
                diskCache.fileSystem.write(editor.metadata) {
                    writeUtf8("$uriString\n")
                    writeUtf8("$width\n")
                    writeUtf8("$height\n")
                }
                diskCache.fileSystem.write(editor.data) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        bitmap.compress(
                            Bitmap.CompressFormat.WEBP_LOSSY,
                            95,
                            outputStream(),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        bitmap.compress(
                            Bitmap.CompressFormat.WEBP,
                            95,
                            outputStream(),
                        )
                    }
                }
                editor.commit()
            }
            val resultImage = bitmap.asImage(shareable = true)
            memoryCache[memoryCacheKey] = resultImage
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

inline val Context.imageMemoryCache: LruMemoryCache
    get() = LruMemoryCache.instance(this)

class LruMemoryCache private constructor(private val maxSizeBytes: Long): ComponentCallbacks2 {

    operator fun set(
        key: Key,
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

    operator fun get(key: Key): Image? =
        imageCache[key]

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                imageCache.evictAll()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
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

    @Suppress("NOTHING_TO_INLINE")
    private inline operator fun LruCache<Key, Image>.set(
        key: Key,
        image: Image,
    ) {
        put(
            key,
            image,
        )
    }

    private val imageCache: LruCache<Key, Image> = CacheImpl()
    private val keyCache: MutableMap<String, Key> = LinkedHashMap()

    data class Key(
        val uri: String,
        val width: Int,
        val height: Int,
    )

    private inner class CacheImpl: LruCache<Key, Image>(checkOverflow(maxSizeBytes)) {

        override fun sizeOf(
            key: Key,
            value: Image,
        ): Int =
            checkOverflow(value.size)

        override fun entryRemoved(
            evicted: Boolean,
            key: Key,
            oldValue: Image,
            newValue: Image?,
        ) {
            synchronized(keyCache) {
                if (keyCache[key.uri] == key) {
                    keyCache.remove(key.uri)
                }
            }
        }
    }

    private fun checkOverflow(value: Long): Int =
        if (value > Int.MAX_VALUE) {
            Int.MAX_VALUE
        } else {
            value.toInt()
        }

    companion object {

        fun instance(context: Context): LruMemoryCache {
            var value = instance
            if (value !== null) {
                return value
            }
            synchronized(this) {
                value = instance
                if (value === null) {
                    val appContext = context.applicationContext
                    value = LruMemoryCache(appContext.getMaxMemory() / 4L)
                    appContext.registerComponentCallbacks(value)
                    instance = value
                }
                return value
            }
        }

        @Volatile
        private var instance: LruMemoryCache? = null
    }
}

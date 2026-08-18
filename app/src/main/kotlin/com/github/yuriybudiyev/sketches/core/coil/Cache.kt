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

fun ImageRequest.Builder.allowLocalCacheIntercept(allow: Boolean): ImageRequest.Builder {
    extras[AllowLocalCacheInterceptKey] = allow
    return this
}

private val AllowLocalCacheInterceptKey: Extras.Key<Boolean> = Extras.Key(default = false)

class LocalCacheInterceptor(private val diskCache: DiskCache): Interceptor {

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
        /*val memoryCacheKey = MemoryCache.Key(
            key = uriString,
            extras = buildMap {
                this[Target] = cacheTarget
                this["width"] = width.toString()
                this["height"] = height.toString()
            },
        )*/
        val diskCacheKey = "$uriString/$width/$height"
        /*val memoryImage = memoryCache[memoryCacheKey]?.image
        if (memoryImage != null) {
            return SuccessResult(
                image = memoryImage,
                request = request,
                dataSource = DataSource.MEMORY_CACHE,
                memoryCacheKey = memoryCacheKey,
                diskCacheKey = diskCacheKey,
                isSampled = false,
                isPlaceholderCached = false,
            )
        }*/
        diskCache.openSnapshot(diskCacheKey)?.use { snapshot ->
            val bitmap = diskCache.fileSystem.read(snapshot.data) {
                BitmapFactory.decodeStream(inputStream())
            }
            if (bitmap != null) {
                val diskImage = bitmap.asImage(shareable = true)
                //memoryCache[memoryCacheKey] = MemoryCache.Value(diskImage)
                return SuccessResult(
                    image = diskImage,
                    request = request,
                    dataSource = DataSource.DISK,
                    memoryCacheKey = null,
                    diskCacheKey = diskCacheKey,
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
                            90,
                            outputStream(),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        bitmap.compress(
                            Bitmap.CompressFormat.WEBP,
                            90,
                            outputStream(),
                        )
                    }
                }
                editor.commit()
            }
            val resultImage = bitmap.asImage(shareable = true)
            //memoryCache[memoryCacheKey] = MemoryCache.Value(resultImage)
            return SuccessResult(
                image = resultImage,
                request = request,
                dataSource = DataSource.DISK,
                memoryCacheKey = null,
                diskCacheKey = diskCacheKey,
                isSampled = false,
                isPlaceholderCached = false,
            )
        }
        return result
    }
}

class LruMemoryCache(private val maxSizeBytes: Long) {

    fun put(
        uri: String,
        width: Int,
        height: Int,
        image: Image,
    ) {
        val key = Key(
            uri = uri,
            width = width,
            height = height,
        )
        imageCache[key] = image
        synchronized(placeholderKeyCache) {
            val placeholder = placeholderKeyCache[uri]
            if (placeholder == null || key.width * key.height > placeholder.width * placeholder.height) {
                placeholderKeyCache[uri] = key
            }
        }
    }

    fun getPlaceholder(uri: String): Image? {
        val placeholderKey = synchronized(placeholderKeyCache) {
            placeholderKeyCache[uri]
        } ?: return null
        return imageCache[placeholderKey]
    }

    fun getImage(
        uri: String,
        width: Int,
        height: Int,
    ): Image? {
        val key = Key(
            uri = uri,
            width = width,
            height = height,
        )
        return imageCache[key]
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
    private val placeholderKeyCache: MutableMap<String, Key> = HashMap()

    data class Key(
        val uri: String,
        val width: Int,
        val height: Int,
    )

    private inner class CacheImpl: LruCache<Key, Image>((maxSizeBytes / 8L).toInt()) {

        override fun sizeOf(
            key: Key,
            value: Image,
        ): Int = (value.size / 8L).toInt()

        override fun entryRemoved(
            evicted: Boolean,
            key: Key,
            oldValue: Image,
            newValue: Image?,
        ) {
            synchronized(placeholderKeyCache) {
                if (placeholderKeyCache[key.uri] == key) {
                    placeholderKeyCache.remove(key.uri)
                }
            }
        }
    }
}

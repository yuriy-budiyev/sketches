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

import kotlin.math.max
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.collection.LruCache
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
import coil3.size.Size
import coil3.toBitmap
import com.github.yuriybudiyev.sketches.core.math.floorEven
import com.github.yuriybudiyev.sketches.core.math.toIntClamped
import com.github.yuriybudiyev.sketches.core.platform.memory.getMaxMemory

fun ImageRequest.Builder.allowLocalDiskCache(allow: Boolean): ImageRequest.Builder {
    extras[AllowLocalDiskCacheKey] = allow
    return this
}

class LocalCacheInterceptor(
    private val memoryCache: ImageMemoryCache,
    private val diskCache: DiskCache,
): Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        if (request.extras[AllowLocalDiskCacheKey] != true) {
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
        val chainSize = chain.size
        val chainWidth = (chainSize.width as? Dimension.Pixels)?.px ?: return chain.proceed()
        val chainHeight = (chainSize.height as? Dimension.Pixels)?.px ?: return chain.proceed()
        val requiredSize = max(chainWidth, chainHeight)
        val hardwareBitmapsAllowed = request.allowHardware
        val memoryEntry = memoryCache[uri]
        if (memoryEntry != null) {
            val memoryImage = memoryEntry.image
            val sampled = memoryEntry.isSampled
            if (
                (!sampled || memoryImage.isSizeFits(requiredSize)) &&
                (hardwareBitmapsAllowed || !memoryImage.isHardwareBitmap())
            ) {
                return SuccessResult(
                    image = memoryImage,
                    request = request,
                    dataSource = DataSource.MEMORY_CACHE,
                    memoryCacheKey = null,
                    diskCacheKey = null,
                    isSampled = sampled,
                    isPlaceholderCached = false,
                )
            }
        }
        diskCache.openSnapshot(uri)?.use { snapshot ->
            val sampled = diskCache.fileSystem.read(snapshot.metadata) {
                val lines = readString(Charsets.UTF_8).lines()
                var sampled = true
                if (lines.size >= 4) {
                    val sampledLine = lines[3]
                    if (sampledLine.isNotEmpty()) {
                        sampled = sampledLine.toBoolean()
                    }
                }
                return@read sampled
            }
            val bitmap = diskCache.fileSystem.read(snapshot.data) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeStream(peek().inputStream(), null, options)
                val outWidth = options.outWidth
                val outHeight = options.outHeight
                if (sampled && !checkSizeFits(outWidth, outHeight, requiredSize)) {
                    return@read null
                }
                options.inJustDecodeBounds = false
                options.inMutable = false
                options.inPreferredConfig =
                    if (hardwareBitmapsAllowed) {
                        Bitmap.Config.HARDWARE
                    } else {
                        if (options.outConfig == Bitmap.Config.RGBA_F16) {
                            Bitmap.Config.RGBA_F16
                        } else {
                            Bitmap.Config.ARGB_8888
                        }
                    }
                return@read BitmapFactory.decodeStream(inputStream(), null, options)
            }
            if (bitmap != null) {
                val diskImage = bitmap.asImage(shareable = !bitmap.isMutable)
                diskImage.bitmap.prepareToDraw()
                memoryCache[uri] = ImageMemoryCache.Entry(diskImage, sampled)
                return SuccessResult(
                    image = diskImage,
                    request = request,
                    dataSource = DataSource.DISK,
                    memoryCacheKey = null,
                    diskCacheKey = null,
                    isSampled = sampled,
                    isPlaceholderCached = false,
                )
            }
        }
        val result = chain.withSize(Size(requiredSize, requiredSize)).proceed()
        if (result is SuccessResult) {
            val bitmap = result.image.toBitmap()
            val width = bitmap.width
            val height = bitmap.height
            val sampled = result.isSampled
            diskCache.openEditor(uri)?.let { editor ->
                diskCache.fileSystem.write(editor.metadata, mustCreate = false) {
                    writeUtf8("$uri\n")
                    writeUtf8("$width\n")
                    writeUtf8("$height\n")
                    writeUtf8("$sampled\n")
                }
                diskCache.fileSystem.write(editor.data, mustCreate = false) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        bitmap.compress(
                            Bitmap.CompressFormat.WEBP_LOSSY,
                            DiskCacheImageQuality,
                            outputStream(),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        bitmap.compress(
                            Bitmap.CompressFormat.WEBP,
                            DiskCacheImageQuality,
                            outputStream(),
                        )
                    }
                }
                editor.commit()
            }
            val resultImage = bitmap.asImage(shareable = !bitmap.isMutable)
            resultImage.bitmap.prepareToDraw()
            memoryCache[uri] = ImageMemoryCache.Entry(resultImage, sampled)
            return SuccessResult(
                image = resultImage,
                request = request,
                dataSource = DataSource.DISK,
                memoryCacheKey = null,
                diskCacheKey = null,
                isSampled = sampled,
                isPlaceholderCached = false,
            )
        }
        return result
    }

    companion {

        private const val DiskCacheImageQuality: Int = 90
    }
}

inline val Context.imageMemoryCache: ImageMemoryCache
    get() = ImageMemoryCache.get(this)

class ImageMemoryCache private constructor(maxSize: Int) {

    operator fun set(uri: String, image: Entry) {
        imageCache[uri] = image
    }

    operator fun get(uri: String): Entry? = imageCache[uri]

    private val imageCache: LruCache<String, Entry> = CacheImpl(maxSize)
    private val memoryCallbacks: ComponentCallbacks2 = CallbacksImpl()

    private class CacheImpl(maxSize: Int): LruCache<String, Entry>(maxSize) {

        override fun sizeOf(key: String, value: Entry): Int = value.image.size.toIntClamped()
    }

    private inner class CallbacksImpl: ComponentCallbacks2 {

        override fun onTrimMemory(level: Int) {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                imageCache.trimToSize(imageCache.maxSize() / 2)
            }
        }

        override fun onConfigurationChanged(newConfig: Configuration) {
            /* no-op */
        }

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() {
            /* no-op */
        }
    }

    data class Entry(
        val image: Image,
        val isSampled: Boolean,
    )

    companion {

        fun get(context: Context): ImageMemoryCache {
            var instance = _instance
            if (instance !== null) {
                return instance
            }
            synchronized(_instanceLock) {
                instance = _instance
                if (instance === null) {
                    val appContext = context.applicationContext
                    instance = ImageMemoryCache(floorEven((appContext.getMaxMemory() / 4L).toIntClamped()))
                    appContext.registerComponentCallbacks(instance.memoryCallbacks)
                    _instance = instance
                }
                return instance
            }
        }

        @Volatile
        private var _instance: ImageMemoryCache? = null

        private val _instanceLock: Any = Any()
    }
}

private val AllowLocalDiskCacheKey: Extras.Key<Boolean> = Extras.Key(default = false)

@Suppress("NOTHING_TO_INLINE")
private inline operator fun <K: Any, V: Any> LruCache<K, V>.set(key: K, value: V) {
    put(key, value)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Image.isHardwareBitmap(): Boolean =
    this is BitmapImage && bitmap.config == Bitmap.Config.HARDWARE

@Suppress("NOTHING_TO_INLINE")
private inline fun Image.isSizeFits(targetSize: Int): Boolean =
    checkSizeFits(width, height, targetSize)

@Suppress("NOTHING_TO_INLINE")
private inline fun checkSizeFits(width: Int, height: Int, targetSize: Int): Boolean {
    val threshold = targetSize - 4 // Allow 4px inaccuracy, result image dimensions can differ from requested.
    return width >= threshold && height >= threshold
}

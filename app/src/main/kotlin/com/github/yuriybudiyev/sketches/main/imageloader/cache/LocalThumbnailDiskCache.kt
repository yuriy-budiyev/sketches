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

package com.github.yuriybudiyev.sketches.main.imageloader.cache

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import coil3.asImage
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.toBitmap
import com.github.yuriybudiyev.sketches.core.ui.components.media.cache.SketchesMemoryCacheKeys
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class LocalThumbnailDiskCache(
    private val memoryCache: MemoryCache,
    private val diskCache: DiskCache,
): Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        if (
            request.memoryCacheKeyExtras[SketchesMemoryCacheKeys.Extras.LocalDiskCache]
            != SketchesMemoryCacheKeys.LocalDiskCache.Allow
        ) {
            return chain.proceed()
        }
        val data = request.data as? Uri ?: return chain.proceed()
        val dataScheme = data.scheme
        if (dataScheme != "content" && dataScheme != "file") {
            return chain.proceed()
        }
        val size = chain.size
        val widthDimension = size.width as? Dimension.Pixels ?: return chain.proceed()
        val heightDimension = size.height as? Dimension.Pixels ?: return chain.proceed()
        val cacheKey = "thumbnail_${data}_size_${widthDimension.px}x${heightDimension.px}"
        diskCache.openSnapshot(cacheKey)?.use { snapshot ->
            val bitmap = snapshot.data
                .toNioPath()
                .inputStream()
                .buffered(bufferSize)
                .use { inputStream -> BitmapFactory.decodeStream(inputStream) }
            if (bitmap != null) {
                val image = bitmap.asImage(shareable = true)
                val memoryCacheKey = request.memoryCacheKey?.let { memoryCacheKey ->
                    val key = MemoryCache.Key(
                        memoryCacheKey,
                        request.memoryCacheKeyExtras,
                    )
                    memoryCache[key] = MemoryCache.Value(image)
                    return@let key
                }
                return SuccessResult(
                    image = image,
                    request = request,
                    dataSource = DataSource.DISK,
                    memoryCacheKey = memoryCacheKey,
                    diskCacheKey = cacheKey,
                    isSampled = true,
                    isPlaceholderCached = request
                        .placeholderMemoryCacheKey
                        ?.let { key -> memoryCache[key] != null }
                        ?: false,
                )
            }
        }
        val result = chain.proceed()
        if (result is SuccessResult) {
            val bitmap = result.image.toBitmap()
            diskCache
                .openEditor(cacheKey)
                ?.commitAndOpenSnapshot()
                ?.use { snapshot ->
                    snapshot.data
                        .toNioPath()
                        .outputStream()
                        .buffered(bufferSize)
                        .use { outputStream ->
                            bitmap.compress(
                                Bitmap.CompressFormat.PNG,
                                100,
                                outputStream,
                            )
                        }
                }
            return result.copy(
                image = bitmap.asImage(shareable = true),
                diskCacheKey = cacheKey,
            )
        }
        return result
    }

    private val bufferSize: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            16384
        } else {
            8192
        }
}

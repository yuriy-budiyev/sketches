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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import coil3.Extras
import coil3.asImage
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.imageLoader
import coil3.intercept.Interceptor
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.toBitmap

fun ImageRequest.Builder.requestTarget(requestTarget: RequestTarget): ImageRequest.Builder {
    extras[RequestTargetKey] = requestTarget
    return this
}

fun ImageRequest.Builder.placeholder(
    uri: Uri,
    context: Context,
): ImageRequest.Builder {
    val memoryCache = context.imageLoader.memoryCache ?: return this
    val primaryKey = primaryPlaceholderKey(uri)
    val secondaryKey = secondaryPlaceholderKey(uri)
    placeholder {
        memoryCache[primaryKey]?.image ?: memoryCache[secondaryKey]?.image
    }
    return this
}

fun primaryPlaceholderKey(uri: Uri): MemoryCache.Key =
    MemoryCache.Key(
        key = uri.toString(),
        extras = buildMap {
            this[Target] = GalleryTarget
        },
    )

fun secondaryPlaceholderKey(uri: Uri): MemoryCache.Key =
    MemoryCache.Key(
        key = uri.toString(),
        extras = buildMap {
            this[Target] = MediaBarTarget
        },
    )

enum class RequestTarget {
    Preview,
    Gallery,
    MediaBar,
}

private val RequestTargetKey: Extras.Key<RequestTarget?> = Extras.Key(default = null)

private const val Target: String = "target"
private const val GalleryTarget: String = "gallery"
private const val MediaBarTarget: String = "media-bar"

class LocalCacheInterceptor(
    private val memoryCache: MemoryCache,
    private val diskCache: DiskCache,
): Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val uri = request.data as? Uri ?: return chain.proceed()
        val uriScheme = uri.scheme
        if (uriScheme != "content" && uriScheme != "file") {
            return chain.proceed()
        }
        val extras = request.extras
        val requestTarget = extras[RequestTargetKey] ?: return chain.proceed()
        val cacheTarget = when (requestTarget) {
            RequestTarget.Preview -> return chain.proceed()
            RequestTarget.Gallery -> GalleryTarget
            RequestTarget.MediaBar -> MediaBarTarget
        }
        val size = chain.size
        val width = (size.width as? Dimension.Pixels)?.px ?: return chain.proceed()
        val height = (size.height as? Dimension.Pixels)?.px ?: return chain.proceed()
        val uriString = uri.toString()
        val memoryCacheKey = MemoryCache.Key(
            key = uriString,
            extras = buildMap {
                this[Target] = cacheTarget
                this["width"] = width.toString()
                this["height"] = height.toString()
            },
        )
        val previewMemoryCacheKey = MemoryCache.Key(
            key = uriString,
            extras = buildMap {
                this[Target] = cacheTarget
            },
        )
        val diskCacheKey = "$uriString/$cacheTarget/$width/$height"
        val memoryCacheValue = memoryCache[memoryCacheKey]
        if (memoryCacheValue != null) {
            memoryCache[previewMemoryCacheKey] = memoryCacheValue
            return SuccessResult(
                image = memoryCacheValue.image,
                request = request,
                dataSource = DataSource.MEMORY_CACHE,
                memoryCacheKey = memoryCacheKey,
                diskCacheKey = diskCacheKey,
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
                val cacheValue = MemoryCache.Value(diskImage)
                memoryCache[memoryCacheKey] = cacheValue
                memoryCache[previewMemoryCacheKey] = cacheValue
                return SuccessResult(
                    image = diskImage,
                    request = request,
                    dataSource = DataSource.MEMORY_CACHE,
                    memoryCacheKey = memoryCacheKey,
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
                    writeUtf8("$cacheTarget\n")
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
            val cacheValue = MemoryCache.Value(resultImage)
            memoryCache[memoryCacheKey] = cacheValue
            memoryCache[previewMemoryCacheKey] = cacheValue
            return SuccessResult(
                image = resultImage,
                request = request,
                dataSource = DataSource.MEMORY_CACHE,
                memoryCacheKey = memoryCacheKey,
                diskCacheKey = diskCacheKey,
                isSampled = false,
                isPlaceholderCached = false,
            )
        }
        return result
    }
}

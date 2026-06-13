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

package com.github.yuriybudiyev.sketches.core.ui.components.media.cache

import android.net.Uri
import androidx.compose.runtime.Stable
import coil3.memory.MemoryCache

object SketchesMemoryCacheKeys {

    @Stable
    fun thumbnail(uri: Uri): MemoryCache.Key =
        MemoryCache.Key(
            key = uri.toString(),
            extras = thumbnailExtras,
        )

    @Stable
    fun mediaBar(uri: Uri): MemoryCache.Key =
        MemoryCache.Key(
            key = uri.toString(),
            extras = mediaBarExtras,
        )

    @Stable
    fun preview(uri: Uri): MemoryCache.Key =
        MemoryCache.Key(
            key = uri.toString(),
            extras = previewExtras,
        )

    private val thumbnailExtras: Map<String, String> = buildMap {
        put(
            key = Extras.Purpose,
            value = Purpose.Thumbnail,
        )
        put(
            key = Extras.LocalDiskCache,
            value = LocalDiskCache.Allow,
        )
    }

    private val mediaBarExtras: Map<String, String> = buildMap {
        put(
            key = Extras.Purpose,
            value = Purpose.MediaBar,
        )
        put(
            key = Extras.LocalDiskCache,
            value = LocalDiskCache.Allow,
        )
    }

    private val previewExtras: Map<String, String> = buildMap {
        put(
            key = Extras.Purpose,
            value = Purpose.Preview,
        )
        put(
            key = Extras.LocalDiskCache,
            value = LocalDiskCache.Disallow,
        )
    }

    object Extras {

        const val Purpose = "purpose"
        const val LocalDiskCache = "local_disk_cache"
    }

    object Purpose {

        const val Thumbnail = "thumbnail"
        const val Preview = "preview"
        const val MediaBar = "media_bar"
    }

    object LocalDiskCache {

        const val Allow = "allow"
        const val Disallow = "disallow"

        fun checkAllow(extras: Map<String, String>): Boolean =
            extras[Extras.LocalDiskCache] == Allow
    }
}

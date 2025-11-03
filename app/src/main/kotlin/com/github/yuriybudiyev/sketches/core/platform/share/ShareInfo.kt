/*
 * MIT License
 *
 * Copyright (c) 2025 Yuriy Budiyev
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

package com.github.yuriybudiyev.sketches.core.platform.share

import android.net.Uri
import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import java.util.EnumSet

data class ShareInfo(
    val uris: ArrayList<Uri>,
    val mimeType: String,
)

fun Collection<MediaStoreFile>.toShareInfo(): ShareInfo {
    val size = this.size
    val uris = ArrayList<Uri>(size)
    if (size == 0) {
        return ShareInfo(
            uris = uris,
            mimeType = "*/*",
        )
    }
    if (size == 1) {
        val file = this.first()
        uris.add(file.uri)
        return ShareInfo(
            uris = uris,
            mimeType = file.mimeType,
        )
    }
    val mediaTypes = EnumSet.noneOf(MediaType::class.java)
    for (file in this) {
        uris.add(file.uri)
        mediaTypes.add(file.mediaType)
    }
    return ShareInfo(
        uris = uris,
        mimeType = if (mediaTypes.size == 1) {
            mediaTypes.first().mimeType
        } else {
            "*/*"
        },
    )
}

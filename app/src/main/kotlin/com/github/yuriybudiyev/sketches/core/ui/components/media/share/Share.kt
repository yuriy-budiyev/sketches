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

package com.github.yuriybudiyev.sketches.core.ui.components.media.share

import com.github.yuriybudiyev.sketches.core.data.model.MediaStoreFile
import com.github.yuriybudiyev.sketches.core.platform.content.MediaType
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.MediaDescriptor
import com.github.yuriybudiyev.sketches.core.ui.components.media.batch.toMediaDescriptor
import java.util.EnumSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun Collection<MediaStoreFile>.prepareForSharing(
    filterIds: Set<Long>,
    mediaSizeLimit: Int,
    onDataReady: (media: List<MediaDescriptor>, mimeType: String) -> Unit,
) {
    contract {
        callsInPlace(
            onDataReady,
            InvocationKind.EXACTLY_ONCE,
        )
    }
    val size = this.size.coerceAtMost(filterIds.size).coerceAtMost(mediaSizeLimit)
    if (size == 0) {
        onDataReady(
            emptyList(),
            "*/*",
        )
        return
    }
    val media = ArrayList<MediaDescriptor>(size)
    val mediaTypes = EnumSet.noneOf(MediaType::class.java)
    for (file in this) {
        if (filterIds.contains(file.id)) {
            media.add(file.toMediaDescriptor())
            mediaTypes.add(file.mediaType)
        }
        if (media.size == size) {
            break
        }
    }
    onDataReady(
        media,
        if (mediaTypes.size == 1) {
            mediaTypes.first().mimeType
        } else {
            "*/*"
        },
    )
}

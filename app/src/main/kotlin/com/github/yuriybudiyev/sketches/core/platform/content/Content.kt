/*
 * MIT License
 *
 * Copyright (c) 2024 Yuriy Budiyev
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

package com.github.yuriybudiyev.sketches.core.platform.content

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.RequiresApi
import com.github.yuriybudiyev.sketches.core.data.model.MediaType

fun MediaType.contentUri(): Uri =
    when (this) {
        MediaType.Image ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        MediaType.Video ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
    }

fun MediaType.mimeType(): String =
    when (this) {
        MediaType.Image -> "image/*"
        MediaType.Video -> "video/*"
    }

@RequiresApi(Build.VERSION_CODES.R)
fun ActivityResultLauncher<IntentSenderRequest>.launchDeleteMediaRequest(
    context: Context,
    uris: Collection<Uri>,
) {
    launch(
        IntentSenderRequest
            .Builder(MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender)
            .build(),
    )
}

fun ActivityResultLauncher<IntentSenderRequest>.launchDeleteMediaRequestOrThrow(
    context: Context,
    uris: Collection<Uri>,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        launchDeleteMediaRequest(context, uris)
    } else {
        error("Low SDK version: ${Build.VERSION.SDK_INT}")
    }
}

const val MediaStoreBatchSize: Int = 500

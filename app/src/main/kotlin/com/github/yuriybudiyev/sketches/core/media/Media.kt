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

package com.github.yuriybudiyev.sketches.core.media

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.DeprecatedSinceApi
import coil3.size.Size
import com.github.yuriybudiyev.sketches.core.math.floorEven
import com.github.yuriybudiyev.sketches.core.platform.memory.getMaxMemory
import kotlin.math.sqrt

/**
 * Max [Bitmap] size assuming most images have 32 bits per pixel,
 * allowing pixels to take 15% of max memory.
 */
fun Context.getMaxBitmapSize(): Size {
    val size = floorEven(sqrt(getMaxMemory().toDouble() * 0.0375).toInt())
    return Size(width = size, height = size)
}

/**
 * Max media buffer size for single player instance (15% of max memory).
 */
fun Context.getMaxMediaBufferSize(): Int =
    floorEven((getMaxMemory().toDouble() * 0.15).toInt())

/**
 * Create blurred [Bitmap] from [source] with blur [radius] using [RenderScript].
 */
@Suppress("DEPRECATION")
@DeprecatedSinceApi(Build.VERSION_CODES.S, "RenderScript has been deprecated in API level 31")
fun createBlurredBitmap(context: Context, source: Bitmap, radius: Float): Bitmap {
    require(radius > 0F && radius <= 25F) { "Radius out of range (0 < radius <= 25)" }
    val renderScript = RenderScript.create(context)
    val workBitmap = source.copy(Bitmap.Config.ARGB_8888, true)
    val input = Allocation.createFromBitmap(
        renderScript,
        workBitmap,
        Allocation.MipmapControl.MIPMAP_NONE,
        Allocation.USAGE_SCRIPT,
    )
    val output = Allocation.createTyped(renderScript, input.type)
    val blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
    blurScript.setRadius(radius)
    blurScript.setInput(input)
    blurScript.forEach(output)
    output.copyTo(workBitmap)
    blurScript.destroy()
    output.destroy()
    input.destroy()
    renderScript.destroy()
    return workBitmap
}

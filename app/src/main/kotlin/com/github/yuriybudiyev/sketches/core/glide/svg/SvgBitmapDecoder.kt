/*
 * MIT License
 *
 * Copyright (c) 2023 Yuriy Budiyev
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

package com.github.yuriybudiyev.sketches.core.glide.svg

import android.graphics.Bitmap
import android.graphics.Canvas
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.request.target.Target
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import java.io.IOException
import java.io.InputStream

class SvgBitmapDecoder(private val bitmapPool: BitmapPool) : ResourceDecoder<InputStream, Bitmap> {

    override fun handles(
        source: InputStream,
        options: Options
    ): Boolean =
        true

    override fun decode(
        source: InputStream,
        width: Int,
        height: Int,
        options: Options
    ): Resource<Bitmap> {
        try {
            val svg = SVG.getFromInputStream(source)
            val originalWidth: Float = svg.documentWidth
            val originalHeight: Float = svg.documentHeight
            if (svg.documentViewBox == null) {
                svg.setDocumentViewBox(
                    0f,
                    0f,
                    originalWidth,
                    originalHeight
                )
            }
            val outWidth: Int
            val outHeight: Int
            when {
                width < height -> {
                    val baseWidth: Float =
                        if (width == Target.SIZE_ORIGINAL) originalWidth else width.toFloat()
                    outWidth = baseWidth.toInt()
                    outHeight = (baseWidth / originalWidth * originalHeight).toInt()
                }
                width > height -> {
                    val baseHeight: Float =
                        if (height == Target.SIZE_ORIGINAL) originalHeight else height.toFloat()
                    outWidth = (baseHeight / originalHeight * originalWidth).toInt()
                    outHeight = baseHeight.toInt()
                }
                else -> {
                    outWidth = if (width == Target.SIZE_ORIGINAL) originalWidth.toInt() else width
                    outHeight =
                        if (height == Target.SIZE_ORIGINAL) originalHeight.toInt() else height
                }
            }
            svg.documentWidth = outWidth.toFloat()
            svg.documentHeight = outHeight.toFloat()
            val bitmap = Bitmap.createBitmap(
                outWidth,
                outHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            svg.renderToCanvas(canvas)
            return BitmapResource(
                bitmap,
                bitmapPool
            )
        } catch (e: SVGParseException) {
            throw IOException(e)
        }
    }
}

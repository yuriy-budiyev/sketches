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

package com.github.yuriybudiyev.sketches.core.collections

import android.os.Build
import kotlin.math.ceil

/**
 * Backport of [java.util.LinkedHashSet.newLinkedHashSet] for older APIs
 */
fun <E> newLinkedHashSet(numElements: Int): LinkedHashSet<E> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        java.util.LinkedHashSet.newLinkedHashSet(numElements)
    } else {
        java.util.LinkedHashSet(
            ceil(numElements.toDouble() / 0.75).toInt(),
            0.75F,
        )
    }

/**
 * Backport of [java.util.LinkedHashMap.newLinkedHashMap] for older APIs
 */
fun <K, V> newLinkedHashMap(numElements: Int): LinkedHashMap<K, V> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        java.util.LinkedHashMap.newLinkedHashMap(numElements)
    } else {
        java.util.LinkedHashMap(
            ceil(numElements.toDouble() / 0.75).toInt(),
            0.75F,
        )
    }

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

package com.github.yuriybudiyev.sketches.core.platform.memory

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * Maximum available memory in bytes
 */
fun Context.getMaxMemory(): Long {
    try {
        val activityManager = getSystemService(ActivityManager::class.java)
        val largeHeap = (applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
        val memoryClass =
            if (largeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
        return memoryClass.toLong() * 1024L * 1024L
    } catch (_: Exception) {
        return Runtime.getRuntime().maxMemory()
    }
}

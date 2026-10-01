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

package com.github.yuriybudiyev.sketches.core.math

/**
 * Closest even value to this integer [x] towards positive infinity.
 */
fun closestEven(x: Int): Int =
    x + 1 and -2

/**
 * Converts this [Long] value to [Int].
 *
 * Clamps this `Long` value in [Int.MIN_VALUE]..[Int.MAX_VALUE] range.
 */
fun Long.toIntClamped(): Int =
    when {
        this >= Int.MAX_VALUE.toLong() -> Int.MAX_VALUE
        this <= Int.MIN_VALUE.toLong() -> Int.MIN_VALUE
        else -> this.toInt()
    }

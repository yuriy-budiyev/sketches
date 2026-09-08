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

package com.github.yuriybudiyev.sketches.core.platform.log

import android.util.Log
import com.github.yuriybudiyev.sketches.BuildConfig
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Send a debug log [message] with given [tag] and [throwable] if [BuildConfig.DEBUG] is `true`
 */
@Suppress("UNUSED")
@OptIn(ExperimentalContracts::class)
inline fun logDebug(
    tag: () -> String = { "SketchesDebug" },
    throwable: () -> Throwable? = { null },
    message: () -> Any?,
) {
    contract {
        callsInPlace(tag, InvocationKind.AT_MOST_ONCE)
        callsInPlace(throwable, InvocationKind.AT_MOST_ONCE)
        callsInPlace(message, InvocationKind.AT_MOST_ONCE)
    }
    if (BuildConfig.DEBUG) {
        val tag = tag()
        val throwable = throwable()
        val message = message().toString()
        if (throwable != null) {
            Log.d(tag, message, throwable)
        } else {
            Log.d(tag, message)
        }
    }
}

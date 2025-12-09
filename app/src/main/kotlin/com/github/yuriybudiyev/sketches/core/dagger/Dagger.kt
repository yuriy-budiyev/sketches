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

package com.github.yuriybudiyev.sketches.core.dagger

import javax.inject.Provider
import kotlin.reflect.KProperty

typealias LazyProvider<T> = dagger.Lazy<T>

/**
 * Provides a fully-constructed and injected instance of [T].
 *
 * Returns the underlying value, computing the value if necessary. All calls to
 * the same [LazyProvider] instance will return the same result.
 */
operator fun <T> LazyProvider<T>.getValue(
    thisRef: Any?,
    property: KProperty<*>,
): T =
    get()

/**
 * Provides a fully-constructed and injected instance of [T].
 */
operator fun <T> Provider<T>.getValue(
    thisRef: Any?,
    property: KProperty<*>,
): T =
    get()

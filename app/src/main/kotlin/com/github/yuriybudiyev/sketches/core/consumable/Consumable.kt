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

package com.github.yuriybudiyev.sketches.core.consumable

class Consumable<T> private constructor(value: T) {

    val isConsumed: Boolean
        get() = valueInternal === Consumed

    @Suppress("UNCHECKED_CAST")
    fun consume(): T? {
        var value: Any? = valueInternal
        if (value === Consumed) {
            value = null
        } else {
            synchronized(consumeLock) {
                value = valueInternal
                if (value === Consumed) {
                    value = null
                } else {
                    valueInternal = Consumed
                }
            }
        }
        return value as T?
    }

    @Volatile
    private var valueInternal: Any? = value

    private val consumeLock: Any = Any()

    override fun toString(): String =
        "Consumable($valueInternal)"

    private object Consumed {

        override fun toString(): String =
            "Consumed"
    }

    companion object {

        fun <T> from(value: T): Consumable<T> =
            Consumable(value)

        @Suppress("UNCHECKED_CAST")
        fun <T> consumed(): Consumable<T> =
            Consumable(Consumed) as Consumable<T>
    }
}

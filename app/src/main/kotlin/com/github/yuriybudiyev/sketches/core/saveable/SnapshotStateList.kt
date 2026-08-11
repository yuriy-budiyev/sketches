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

package com.github.yuriybudiyev.sketches.core.saveable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList

@Composable
inline fun <T: Any> rememberSaveableSnapshotStateList(
    vararg inputs: Any?,
    crossinline onInit: SnapshotStateList<T>.() -> Unit = {},
): SnapshotStateList<T> =
    @Suppress("UNCHECKED_CAST")
    rememberSaveable(
        inputs = inputs,
        saver = snapshotStateListSaver(),
    ) {
        SnapshotStateList<T>().apply(onInit)
    }

@Suppress("UNCHECKED_CAST")
fun <T: Any> snapshotStateListSaver(): Saver<SnapshotStateList<T>, ArrayList<T>> =
    SnapshotStateListSaver as Saver<SnapshotStateList<T>, ArrayList<T>>

private object SnapshotStateListSaver: Saver<SnapshotStateList<Any>, ArrayList<Any>> {

    override fun SaverScope.save(value: SnapshotStateList<Any>): ArrayList<Any>? {
        val snapshot = value.toList()
        val snapshotSize = snapshot.size
        if (snapshotSize == 0) {
            return null
        }
        val arrayList = ArrayList<Any>(snapshotSize)
        for (element in snapshot) {
            require(canBeSaved(element)) { "Element can't be saved: $element" }
            arrayList.add(element)
        }
        return arrayList
    }

    override fun restore(value: ArrayList<Any>): SnapshotStateList<Any> {
        val list = SnapshotStateList<Any>()
        list.addAll(value)
        return list
    }
}

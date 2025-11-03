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

package com.github.yuriybudiyev.sketches.core.navigation

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize

class NavResultStore {

    inline fun <reified T: Parcelable> putNavResult(result: T) {
        storage[T::class.qualifiedName!!] = result
    }

    suspend inline fun <reified T: Parcelable> collectNavResult(
        crossinline onResult: suspend (result: T) -> Unit,
    ) {
        val key = T::class.qualifiedName!!
        snapshotFlow { storage.toMap() }
            .map { storage -> storage[key] }
            .collect { value ->
                if (value !== null) {
                    if (storage[key] === value) {
                        storage.remove(key)
                    }
                    onResult(value as T)
                }
            }
    }

    /**
     * Do not use this directly.
     */
    @PublishedApi
    internal val storage: SnapshotStateMap<String, Parcelable> = SnapshotStateMap()
}

val LocalNavResultStore: ProvidableCompositionLocal<NavResultStore> =
    staticCompositionLocalOf { error("CompositionLocal LocalResultStore not present") }

@Composable
fun rememberResultStore(): NavResultStore =
    rememberSaveable(saver = ResultStoreSaver()) { NavResultStore() }

private class ResultStoreSaver: Saver<NavResultStore, ArrayList<KeyValue>> {

    override fun SaverScope.save(value: NavResultStore): ArrayList<KeyValue>? {
        val storage = value.storage
        if (storage.isEmpty()) {
            return null
        }
        val saveable = ArrayList<KeyValue>(storage.size)
        for (entry in storage) {
            saveable.add(
                KeyValue(
                    key = entry.key,
                    value = entry.value,
                )
            )
        }
        return saveable
    }

    override fun restore(value: ArrayList<KeyValue>): NavResultStore =
        NavResultStore().apply {
            for (saved in value) {
                storage[saved.key] = saved.value
            }
        }
}

@Parcelize
private class KeyValue(
    val key: String,
    val value: Parcelable,
): Parcelable

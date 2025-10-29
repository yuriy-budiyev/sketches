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

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.map

class ResultStore {

    inline fun <reified T> setResult(
        key: String,
        value: T?,
    ) {
        results[key] = value
    }

    inline fun <reified T> getResult(key: String): T? =
        results.remove(key) as T?

    inline fun <reified T> getResultFlow(key: String): Flow<T?> =
        snapshotFlow { results }.map { results -> results.remove(key) as T? }

    suspend inline fun <reified T> collectResult(
        key: String,
        collector: FlowCollector<T?>,
    ) {
        getResultFlow<T>(key).collect(collector)
    }

    @PublishedApi
    internal val results: SnapshotStateMap<String, Any?> = SnapshotStateMap()
}

val LocalResultStore: ProvidableCompositionLocal<ResultStore> =
    staticCompositionLocalOf { error("CompositionLocal LocalResultStore not present") }

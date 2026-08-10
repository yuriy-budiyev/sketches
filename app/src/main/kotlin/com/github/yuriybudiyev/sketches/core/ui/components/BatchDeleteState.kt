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

package com.github.yuriybudiyev.sketches.core.ui.components

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.parcelize.Parcelize

@Composable
fun rememberBatchDeleteState(): BatchDeleteState =
    rememberSaveable(saver = BatchDeleteStateImplSaver()) { BatchDeleteStateImpl() }

@Stable
sealed interface BatchDeleteState {

    val action: Flow<Action>

    suspend fun start(uris: List<Uri>)

    suspend fun proceed()

    suspend fun reset()

    sealed interface Action {

        data class Delete(val uris: List<Uri>): Action

        data object Finish: Action

        data object Reset: Action
    }
}

private class BatchDeleteStateImpl: BatchDeleteState {

    override val action: Flow<BatchDeleteState.Action>
        field = MutableSharedFlow()

    override suspend fun start(uris: List<Uri>) {
        allUris = uris
        startIndex = 0
        proceed()
    }

    override suspend fun proceed() {
        val size = allUris.size
        if (size == 0) {
            return
        }
        if (startIndex >= size) {
            startIndex = 0
            allUris = emptyList()
            action.emit(BatchDeleteState.Action.Finish)
            return
        }
        val batchStartIndex = startIndex
        val batchEndIndex = (batchStartIndex + 500).coerceAtMost(size)
        startIndex = batchEndIndex
        action.emit(
            BatchDeleteState.Action.Delete(
                allUris.subList(
                    fromIndex = batchStartIndex,
                    toIndex = batchEndIndex,
                ),
            ),
        )
    }

    override suspend fun reset() {
        startIndex = 0
        allUris = emptyList()
        action.emit(BatchDeleteState.Action.Reset)
    }

    var startIndex: Int = 0
    var allUris: List<Uri> = emptyList()
}

@Parcelize
private data class BatchDeleteStateImplConfig(
    var startIndex: Int,
    var allUris: List<Uri>,
): Parcelable

private class BatchDeleteStateImplSaver: Saver<BatchDeleteStateImpl, BatchDeleteStateImplConfig> {

    override fun SaverScope.save(value: BatchDeleteStateImpl): BatchDeleteStateImplConfig =
        BatchDeleteStateImplConfig(
            startIndex = value.startIndex,
            allUris = value.allUris,
        )

    override fun restore(value: BatchDeleteStateImplConfig): BatchDeleteStateImpl =
        BatchDeleteStateImpl().apply {
            startIndex = value.startIndex
            allUris = value.allUris
        }
}

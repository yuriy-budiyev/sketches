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

package com.github.yuriybudiyev.sketches

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch

@Preview
@Composable
fun TestScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        Image(
            imageVector = Icons.Filled.Photo,
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                {},
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("${16f / 9f}")
            }
        }
    }
}

interface LegacyDataSource<T> {

    fun setOnData(onData: (data: T) -> Unit)
}

class LatestCombinerColdFlow<A, B, R>(
    private val sourceA: LegacyDataSource<A>,
    private val sourceB: LegacyDataSource<B>,
    private val combine: suspend (A, B) -> R
) {

    fun start(): Flow<R> =
        combine(
            flow = sourceA.asFlow(),
            flow2 = sourceB.asFlow(),
            transform = combine
        )

    private fun <T> LegacyDataSource<T>.asFlow(): Flow<T> =
        callbackFlow {
            setOnData { data: T ->
                trySendBlocking(data)
            }
        }
}

class LatestCombinerHotFlow<A, B, R>(
    sourceA: LegacyDataSource<A>,
    sourceB: LegacyDataSource<B>,
    private val coroutineScope: CoroutineScope,
    private val combine: suspend (A, B) -> R
) {

    fun start(): Flow<R> =
        combine(
            flow = hotFlowA,
            flow2 = hotFlowB,
            transform = combine
        )

    fun start(onCombined: suspend (R) -> Unit) {
        coroutineScope.launch {
            start().collect { result ->
                onCombined(result)
            }
        }
    }

    private val hotFlowA: SharedFlow<A> =
        sourceA
            .asFlow()
            .shareIn(
                scope = coroutineScope,
                started = SharingStarted.Lazily,
                replay = 1
            )

    private val hotFlowB: SharedFlow<B> =
        sourceB
            .asFlow()
            .shareIn(
                scope = coroutineScope,
                started = SharingStarted.Lazily,
                replay = 1
            )

    private fun <T> LegacyDataSource<T>.asFlow(): Flow<T> =
        callbackFlow {
            setOnData { data ->
                trySendBlocking(data)
            }
        }
}

class LatestCombinerCallback<A, B, R>(
    private val sourceA: LegacyDataSource<A>,
    private val sourceB: LegacyDataSource<B>,
    private val combine: (A, B) -> R
) {

    fun start(onCombined: (R) -> Unit) {
        val dA = dataA
        val dB = dataB
        if (dA !== NotInit && dB !== NotInit) {
            @Suppress("UNCHECKED_CAST")
            onCombined(
                combine(
                    dA as A,
                    dB as B
                )
            )
        }
        sourceA.setOnData { data ->
            dataA = data
            val sAdB = dataB
            if (sAdB !== NotInit) {
                @Suppress("UNCHECKED_CAST")
                onCombined(
                    combine(
                        data,
                        sAdB as B
                    )
                )
            }
        }
        sourceB.setOnData { data ->
            dataB = data
            val sBdA = dataA
            if (sBdA !== NotInit) {
                @Suppress("UNCHECKED_CAST")
                onCombined(
                    combine(
                        sBdA as A,
                        data
                    )
                )
            }
        }
    }

    private var dataA: Any? = NotInit
    private var dataB: Any? = NotInit

    private object NotInit
}

interface Request

interface Result

interface LegacyApi {

    fun request(
        request: Request,
        onResult: (Result) -> Unit
    )
}

class LegacyApiBatchWrapper(
    private val legacyApi: LegacyApi,
    private val coroutineScope: CoroutineScope
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun request(
        requests: Iterable<Request>,
        onResults: suspend (List<Result>) -> Unit
    ) {
        coroutineScope.launch {
            val results = requests
                .asFlow()
                .flatMapConcat { request ->
                    callbackFlow {
                        legacyApi.request(request) { result ->
                            trySendBlocking(result)
                            close()
                        }
                    }
                }
                .toList()
            onResults(results)
        }
    }
}

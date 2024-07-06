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
 *
 */

package com.github.yuriybudiyev.sketches

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test

class SketchesTest {

    @Test
    fun test(): Unit =
        runBlocking {
            val documents = getDocuments()
            documents.forEach {
                println(it)
            }
        }

    data class Document(val id: String)

    @Suppress("RedundantSuspendModifier")
    suspend fun getDocumentIds(): List<String> {
        var counter = 0
        return listOf(
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter++).toString(),
            (counter).toString(),
        )
    }

    @Suppress("RedundantSuspendModifier")
    suspend fun getDocument(id: String): Document =
        Document(id)

    suspend fun getDocuments(): List<Document> {
        return coroutineScope {
            getDocumentIds()
                .map { id ->
                    async {
                        withContext(Dispatchers.Default) {
                            println("async $id")
                            getDocument(id)
                        }
                    }
                }
                .awaitAll()
        }
    }
}

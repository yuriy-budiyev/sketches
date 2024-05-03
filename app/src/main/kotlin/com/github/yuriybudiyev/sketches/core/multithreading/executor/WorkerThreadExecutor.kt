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

package com.github.yuriybudiyev.sketches.core.multithreading.executor

import android.os.Process
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicLong

class WorkerThreadExecutor(corePoolSize: Int): ScheduledThreadPoolExecutor(
    corePoolSize,
    WorkerThreadFactory,
    RejectedExecutionHandler
) {

    override fun afterExecute(
        r: Runnable,
        t: Throwable?,
    ) {
        if (t == null && r is Future<*> && r.isDone) {
            try {
                r.get()
            } catch (_: CancellationException) {
            } catch (_: InterruptedException) {
            } catch (e: ExecutionException) {
                throw RuntimeException(e.cause)
            }
        }
    }

    init {
        continueExistingPeriodicTasksAfterShutdownPolicy = false
        executeExistingDelayedTasksAfterShutdownPolicy = false
        removeOnCancelPolicy = true
    }

    private object WorkerThreadFactory: ThreadFactory {

        override fun newThread(r: Runnable): Thread =
            WorkerThread(
                r,
                "worker-${counter.getAndIncrement()}"
            )

        private val counter: AtomicLong = AtomicLong(1)

        private class WorkerThread(
            target: Runnable,
            name: String,
        ): Thread(
            target,
            name
        ) {

            override fun run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                super.run()
            }
        }
    }

    private object RejectedExecutionHandler: java.util.concurrent.RejectedExecutionHandler {

        override fun rejectedExecution(
            r: Runnable,
            e: ThreadPoolExecutor,
        ) {
            throw RejectedExecutionException("Task $r rejected from $e")
        }
    }
}

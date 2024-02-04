/*
 * MIT License
 *
 * Copyright (c) 2023 Yuriy Budiyev
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

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.readable
import androidx.compose.runtime.snapshots.withCurrent
import androidx.compose.runtime.snapshots.writable

class Snapshots {

    @Test
    fun snapshots() {
        val range = Range(
            start = 100,
            end = 100
        )
        val snapshot1 = Snapshot.takeMutableSnapshot()
        snapshot1.enter {
            range.start = 50
            range.end = 100
            assertEquals(
                50,
                range.start
            )
            assertEquals(
                100,
                range.end
            )
        }
        assertEquals(
            100,
            range.start
        )
        assertEquals(
            100,
            range.end
        )
        val snapshot2 = Snapshot.takeMutableSnapshot()
        snapshot2.enter {
            range.start = 25
            range.end = 75
            assertEquals(
                25,
                range.start
            )
            assertEquals(
                75,
                range.end
            )
        }
        assertEquals(
            100,
            range.start
        )
        assertEquals(
            100,
            range.end
        )
        snapshot1.enter {
            range.start -= 50
            range.end += 150
            assertEquals(
                0,
                range.start
            )
            assertEquals(
                250,
                range.end
            )
        }
        assertEquals(
            100,
            range.start
        )
        assertEquals(
            100,
            range.end
        )
        snapshot1.apply()
        snapshot1.dispose()
        assertEquals(
            0,
            range.start
        )
        assertEquals(
            250,
            range.end
        )
        snapshot2.dispose()
        val snapshot3 = Snapshot.takeMutableSnapshot()
        snapshot3.enter {
            range.start -= 150
            range.end -= 100
            assertEquals(
                -150,
                range.start
            )
            assertEquals(
                150,
                range.end
            )
        }
        snapshot3.apply()
        snapshot3.dispose()
        assertEquals(
            -150,
            range.start
        )
        assertEquals(
            150,
            range.end
        )
    }
}

class Range(
    start: Int,
    end: Int,
): StateObject {

    init {
        require(start <= end) {
            "Range start should be less than or equal to end ($start, $end)"
        }
    }

    var start: Int
        get() = records.readable(this).start
        set(value) {
            records.withCurrent { record ->
                val recordEnd = record.end
                require(value <= recordEnd) {
                    "Range start should be less than or equal to end ($value, $recordEnd)"
                }
            }
            records.writable(this) {
                start = value
            }
        }

    var end: Int
        get() = records.readable(this).end
        set(value) {
            records.withCurrent { record ->
                val recordStart = record.start
                require(value >= recordStart) {
                    "Range end should be greater than or equal to start ($recordStart, $value)"
                }
            }
            records.writable(this) {
                end = value
            }
        }

    override val firstStateRecord: StateRecord
        get() = records

    override fun prependStateRecord(value: StateRecord) {
        records = value as StateStateRecord
    }

    override fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord,
    ): StateRecord? {
        val currentRecord = current as StateStateRecord
        val appliedRecord = applied as StateStateRecord
        return if (currentRecord.start == appliedRecord.start && currentRecord.end == appliedRecord.end) {
            currentRecord
        } else {
            null
        }
    }

    private var records = StateStateRecord(
        start,
        end
    )

    private class StateStateRecord(
        var start: Int,
        var end: Int,
    ): StateRecord() {

        override fun create(): StateRecord =
            StateStateRecord(
                start,
                end
            )

        override fun assign(value: StateRecord) {
            val record = value as StateStateRecord
            start = record.start
            end = record.end
        }
    }
}

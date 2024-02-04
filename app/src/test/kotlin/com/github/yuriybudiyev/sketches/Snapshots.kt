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
import androidx.compose.runtime.snapshots.writable

class Snapshots {

    @Test
    fun snapshots() {
        val size = Size(
            0,
            0
        )
        val snapshot1 = Snapshot.takeMutableSnapshot()
        snapshot1.enter {
            size.width = 100
            size.height = 50
            assertEquals(
                100,
                size.width
            )
            assertEquals(
                50,
                size.height
            )
        }
        assertEquals(
            0,
            size.width
        )
        assertEquals(
            0,
            size.height
        )
        val snapshot2 = Snapshot.takeMutableSnapshot()
        snapshot2.enter {
            size.width = 25
            size.height = 75
            assertEquals(
                25,
                size.width
            )
            assertEquals(
                75,
                size.height
            )
        }
        assertEquals(
            0,
            size.width
        )
        assertEquals(
            0,
            size.height
        )
        snapshot1.enter {
            size.width += 100
            size.height += 150
            assertEquals(
                200,
                size.width
            )
            assertEquals(
                200,
                size.height
            )
        }
        assertEquals(
            0,
            size.width
        )
        assertEquals(
            0,
            size.height
        )
        snapshot1.apply()
        snapshot1.dispose()
        assertEquals(
            200,
            size.width
        )
        assertEquals(
            200,
            size.height
        )
        snapshot2.dispose()
        val snapshot3 = Snapshot.takeMutableSnapshot()
        snapshot3.enter {
            size.width -= 100
            size.height -= 100
            assertEquals(
                100,
                size.width
            )
            assertEquals(
                100,
                size.height
            )
        }
        snapshot3.apply()
        snapshot3.dispose()
        assertEquals(
            100,
            size.width
        )
        assertEquals(
            100,
            size.height
        )
    }
}

class Size(
    width: Int,
    height: Int,
): StateObject {

    var width: Int
        get() = records.readable(this).width
        set(value) {
            records.writable(this) {
                width = value
            }
        }

    var height: Int
        get() = records.readable(this).height
        set(value) {
            records.writable(this) {
                height = value
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
        return if (currentRecord.width == appliedRecord.width && currentRecord.height == appliedRecord.height) {
            currentRecord
        } else {
            null
        }
    }

    private var records = StateStateRecord(
        width,
        height
    )

    private class StateStateRecord(
        var width: Int,
        var height: Int,
    ): StateRecord() {

        override fun create(): StateRecord =
            StateStateRecord(
                width,
                height
            )

        override fun assign(value: StateRecord) {
            val record = value as StateStateRecord
            width = record.width
            height = record.height
        }
    }
}

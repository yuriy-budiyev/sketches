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

package com.github.yuriybudiyev.sketches.core.data.model

import android.os.Parcel
import android.os.Parcelable

data class MediaStoreBucket(
    val id: Long,
    val name: String,
    val size: Int,
    val coverUri: String,
    val coverDateAdded: Long,
): Parcelable {

    override fun hashCode(): Int =
        id.hashCode()

    override fun equals(other: Any?): Boolean =
        when {
            other === this -> true
            other is MediaStoreBucket -> other.id == this.id
            else -> false
        }

    override fun describeContents(): Int =
        0

    override fun writeToParcel(
        parcel: Parcel,
        flags: Int,
    ) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeInt(size)
        parcel.writeString(coverUri)
        parcel.writeLong(coverDateAdded)
    }

    companion object CREATOR: Parcelable.Creator<MediaStoreBucket?> {

        override fun createFromParcel(parcel: Parcel): MediaStoreBucket? =
            MediaStoreBucket(
                id = parcel.readLong(),
                name = parcel.readString()!!,
                size = parcel.readInt(),
                coverUri = parcel.readString()!!,
                coverDateAdded = parcel.readLong(),
            )

        override fun newArray(size: Int): Array<MediaStoreBucket?> =
            arrayOfNulls(size)
    }
}

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

package com.github.yuriybudiyev.sketches.core.platform.collections

import android.os.Build
import java.util.WeakHashMap
import kotlin.math.ceil

object CollectionsCompat {

    /**
     * Creates a new, empty HashSet suitable for the expected number of elements.
     * The returned set uses the default load factor of 0.75, and its initial capacity is
     * generally large enough so that the expected number of elements can be added
     * without resizing the set.
     *
     * @param numElements the expected number of elements
     * @param E the type of elements maintained by the new set
     */
    fun <E> newHashSet(numElements: Int): HashSet<E> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            HashSet.newHashSet(numElements)
        } else {
            require(numElements >= 0) { "Negative number of elements: $numElements" }
            HashSet(
                calculateHashMapCapacity(numElements),
                DefaultLoadFactor,
            )
        }

    /**
     * Creates a new, empty LinkedHashSet suitable for the expected number of elements.
     * The returned set uses the default load factor of 0.75, and its initial capacity is
     * generally large enough so that the expected number of elements can be added
     * without resizing the set.
     *
     * @param numElements the expected number of elements
     * @param E the type of elements maintained by the new set
     */
    fun <E> newLinkedHashSet(numElements: Int): LinkedHashSet<E> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            LinkedHashSet.newLinkedHashSet(numElements)
        } else {
            require(numElements >= 0) { "Negative number of elements: $numElements" }
            LinkedHashSet(
                calculateHashMapCapacity(numElements),
                DefaultLoadFactor,
            )
        }

    /**
     * Creates a new, empty HashMap suitable for the expected number of mappings.
     * The returned map uses the default load factor of 0.75, and its initial capacity is
     * generally large enough so that the expected number of mappings can be added
     * without resizing the map.
     *
     * @param numMappings the expected number of mappings
     * @param K the type of keys maintained by the new map
     * @param V the type of mapped values
     */
    fun <K, V> newHashMap(numMappings: Int): HashMap<K, V> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            HashMap.newHashMap(numMappings)
        } else {
            require(numMappings >= 0) { "Negative number of mappings: $numMappings" }
            HashMap(
                calculateHashMapCapacity(numMappings),
                DefaultLoadFactor,
            )
        }

    /**
     * Creates a new, empty, insertion-ordered LinkedHashMap suitable for the expected
     * number of mappings. The returned map uses the default load factor of 0.75,
     * and its initial capacity is generally large enough so that the expected
     * number of mappings can be added without resizing the map.
     *
     * @param numMappings the expected number of mappings
     * @param K the type of keys maintained by the new map
     * @param V the type of mapped values
     */
    fun <K, V> newLinkedHashMap(numMappings: Int): LinkedHashMap<K, V> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            LinkedHashMap.newLinkedHashMap(numMappings)
        } else {
            require(numMappings >= 0) { "Negative number of mappings: $numMappings" }
            LinkedHashMap(
                calculateHashMapCapacity(numMappings),
                DefaultLoadFactor,
            )
        }

    /**
     * Creates a new, empty WeakHashMap suitable for the expected number of mappings.
     * The returned map uses the default load factor of 0.75, and its initial capacity is
     * generally large enough so that the expected number of mappings can be added
     * without resizing the map.
     *
     * @param numMappings the expected number of mappings
     * @param K the type of keys maintained by the new map
     * @param V the type of mapped values
     */
    fun <K, V> newWeakHashMap(numMappings: Int): WeakHashMap<K, V> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            WeakHashMap.newWeakHashMap(numMappings)
        } else {
            require(numMappings >= 0) { "Negative number of mappings: $numMappings" }
            WeakHashMap(
                calculateHashMapCapacity(numMappings),
                DefaultLoadFactor,
            )
        }

    private fun calculateHashMapCapacity(numMappings: Int): Int =
        ceil(numMappings.toDouble() / DefaultLoadFactor.toDouble()).toInt()

    private const val DefaultLoadFactor: Float = 0.75f
}

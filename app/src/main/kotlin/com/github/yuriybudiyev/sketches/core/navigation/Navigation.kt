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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope

@Composable
fun rememberNavMenuSpec(): NavMenuSpec {
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    val insets = WindowInsets.navigationBars
    return remember(
        direction,
        density,
        insets,
    ) {
        val paddingValues = insets.asPaddingValues(density)
        val bottomPadding = paddingValues.calculateBottomPadding()
        val startPadding = paddingValues.calculateStartPadding(direction)
        val endPadding = paddingValues.calculateEndPadding(direction)
        val location = when {
            bottomPadding > 0.dp -> NavMenuSpec.Location.Bottom
            startPadding > 0.dp -> NavMenuSpec.Location.Start
            endPadding > 0.dp -> NavMenuSpec.Location.End
            else -> NavMenuSpec.Location.Bottom
        }
        return@remember NavMenuSpec(
            location = location,
            size = when (location) {
                NavMenuSpec.Location.Bottom -> bottomPadding
                NavMenuSpec.Location.Start -> startPadding
                NavMenuSpec.Location.End -> endPadding
            },
        )
    }
}

data class NavMenuSpec(
    val location: Location,
    val size: Dp,
) {

    enum class Location {
        Bottom,
        Start,
        End,
    }
}

inline fun <reified T: NavRoute> EntryProviderScope<NavRoute>.registerNavRoute(
    noinline content: @Composable (T) -> Unit,
) {
    addEntryProvider(
        clazz = T::class,
        clazzContentKey = { navRoute -> navRoute },
        content = content,
    )
}

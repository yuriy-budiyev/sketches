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

package com.github.yuriybudiyev.sketches.feature.buckets.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.core.navigation.destination.TopLevelNavigationDestination
import com.github.yuriybudiyev.sketches.core.ui.icons.SketchesIcons

object BucketsNavigationDestination: TopLevelNavigationDestination {

    override val routeBase: String = "buckets"

    override val arguments: List<NamedNavArgument> = emptyList()

    override val deepLinks: List<NavDeepLink> = emptyList()

    @get:StringRes
    override val titleRes: Int = R.string.buckets

    override val selectedIcon: ImageVector = SketchesIcons.BucketsSelected

    override val unselectedIcon: ImageVector = SketchesIcons.BucketsUnselected
}

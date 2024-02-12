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

package com.github.yuriybudiyev.sketches.feature.image.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavDeepLink
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.github.yuriybudiyev.sketches.core.navigation.destination.NavigationDestination

object ImageNavigationDestination: NavigationDestination {

    override val routeBase: String = "image"

    override val arguments: List<NamedNavArgument> = listOf(
        navArgument(name = Arguments.IMAGE_INDEX) { type = NavType.IntType },
        navArgument(name = Arguments.IMAGE_ID) { type = NavType.LongType },
        navArgument(name = Arguments.BUCKET_ID) { type = NavType.LongType },
    )

    override val deepLinks: List<NavDeepLink> = emptyList()

    object Arguments {

        const val IMAGE_INDEX = "image_index"
        const val IMAGE_ID = "image_id"
        const val BUCKET_ID = "bucket_id"
    }
}

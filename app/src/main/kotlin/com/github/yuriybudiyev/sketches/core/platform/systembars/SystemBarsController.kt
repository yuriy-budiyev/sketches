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

package com.github.yuriybudiyev.sketches.core.platform.systembars

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun SystemBarsVisibility(visible: Boolean) {
    val systemBarsController = LocalSystemBarsController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(
        systemBarsController,
        lifecycleOwner,
        visible,
    ) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (visible) {
                if (!systemBarsController.isSystemBarsVisible) {
                    systemBarsController.showSystemBars()
                }
            } else {
                if (systemBarsController.isSystemBarsVisible) {
                    systemBarsController.hideSystemBars()
                }
            }
        }
    }
}

@Stable
interface SystemBarsController {

    /**
     * Insets ignoring visibility and animations
     */
    val insets: Insets

    val isInMultiWindowMode: Boolean

    val isSystemBarsVisible: Boolean

    fun showSystemBars()

    fun hideSystemBars()

    @Stable
    interface Insets {

        /**
         * Combined insets of [navigationBars], [statusBars] and [captionBar]
         */
        val systemBars: WindowInsets

        /**
         * [WindowInsetsCompat.Type.captionBar]
         */
        val captionBar: WindowInsets

        /**
         * [WindowInsetsCompat.Type.statusBars]
         */
        val statusBars: WindowInsets

        /**
         * [WindowInsetsCompat.Type.navigationBars]
         */
        val navigationBars: WindowInsets
    }
}

val LocalSystemBarsController: ProvidableCompositionLocal<SystemBarsController> =
    staticCompositionLocalOf {
        error("CompositionLocal LocalSystemBarsController not present")
    }

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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

interface RootNavBarController {

    val isRootNavBarVisible: Boolean

    fun showRootNavBar()

    fun hideRootNavBar()
}

val LocalRootNavBarController: ProvidableCompositionLocal<RootNavBarController> =
    staticCompositionLocalOf { error("CompositionLocal LocalRootNavBarController not present") }

@Composable
fun rememberRootNavBarController(): RootNavBarController =
    rememberSaveable(saver = RootNavBarControllerImplSaver()) { RootNavBarControllerImpl() }

private class RootNavBarControllerImpl(): RootNavBarController {

    override var isRootNavBarVisible: Boolean by mutableStateOf(true)

    override fun showRootNavBar() {
        isRootNavBarVisible = true
    }

    override fun hideRootNavBar() {
        isRootNavBarVisible = false
    }
}

private class RootNavBarControllerImplSaver: Saver<RootNavBarControllerImpl, Boolean> {

    override fun SaverScope.save(value: RootNavBarControllerImpl): Boolean =
        value.isRootNavBarVisible

    override fun restore(value: Boolean): RootNavBarControllerImpl {
        val controller = RootNavBarControllerImpl()
        controller.isRootNavBarVisible = value
        return controller
    }
}
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

package com.github.yuriybudiyev.sketches.baselineprofile

import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiSelector
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() {
        baselineProfileRule.collect("com.github.yuriybudiyev.sketches") {
            startActivityAndWait()
            grantMediaPermission()
            scrollImagesGrid()
        }
    }
}

fun MacrobenchmarkScope.grantMediaPermission() {
    val allowButton = device.findObject(
        UiSelector().text(
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "Allow all"
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> "Allow"
                else -> "ALLOW"
            }
        )
    )
    if (allowButton.exists()) {
        allowButton.click()
    }
}

fun MacrobenchmarkScope.scrollImagesGrid() {
    val imagesGrid = device.findObject(By.res("images_grid"))
    imagesGrid.setGestureMargin(device.displayWidth / 5)
    imagesGrid.fling(Direction.DOWN)
    imagesGrid.fling(Direction.UP)
    device.waitForIdle()
}

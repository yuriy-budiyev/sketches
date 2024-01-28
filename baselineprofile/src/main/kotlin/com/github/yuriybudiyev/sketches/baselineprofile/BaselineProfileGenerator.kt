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

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        baselineProfileRule.collect("com.github.yuriybudiyev.sketches") {
            startActivityAndWait()
            grantMediaPermission()
            scrollMedaGrid()
            clickOnImage()
            waitMediaPagerAndPressBack()
            clickOnVideo()
            waitMediaPagerAndPressBack()
        }
    }
}

private fun MacrobenchmarkScope.grantMediaPermission() {
    device.waitForObject(
        By.text(
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "Allow all"
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> "Allow"
                else -> "ALLOW"
            }
        )
    ) { allowButton ->
        allowButton.click()
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.scrollMedaGrid() {
    device.waitForObject(By.res("media_grid")) { mediaGrid ->
        mediaGrid.setGestureMargin(device.displayWidth / 5)
        mediaGrid.fling(Direction.DOWN)
        device.waitForIdle()
        mediaGrid.fling(Direction.UP)
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.clickOnImage() {
    device.waitForObject(By.res("media_item_image")) { mediaItem ->
        mediaItem.click()
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.clickOnVideo() {
    device.waitForObject(By.res("media_item_video")) { mediaItem ->
        mediaItem.click()
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.waitMediaPagerAndPressBack() {
    device.waitForObject(By.res("media_pager")) {
        device.pressBack()
        device.waitForIdle()
    }
}

private fun UiDevice.waitForObject(selector: BySelector): UiObject2? =
    wait(
        Until.findObject(selector),
        Configurator.getInstance().waitForSelectorTimeout
    )

@OptIn(ExperimentalContracts::class)
private inline fun UiDevice.waitForObject(
    selector: BySelector,
    block: (UiObject2) -> Unit,
) {
    contract {
        callsInPlace(block)
    }
    var done = false
    while (!done) {
        val obj = waitForObject(selector)
        if (obj != null) {
            try {
                block(obj)
                done = true
            } catch (_: StaleObjectException) {
            }
        } else {
            done = true
        }
    }
}

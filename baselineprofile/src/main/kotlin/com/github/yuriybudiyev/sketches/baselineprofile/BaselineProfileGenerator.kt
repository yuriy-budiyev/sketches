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

import java.util.regex.Pattern
import kotlin.test.Test
import android.Manifest
import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import com.github.yuriybudiyev.sketches.baselineprofile.utils.PACKAGE_NAME
import com.github.yuriybudiyev.sketches.baselineprofile.utils.waitForObject
import org.junit.Rule

class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        with(InstrumentationRegistry.getInstrumentation().uiAutomation) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    grantRuntimePermission(
                        PACKAGE_NAME,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                    grantRuntimePermission(
                        PACKAGE_NAME,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    grantRuntimePermission(
                        PACKAGE_NAME,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
                else -> {
                    grantRuntimePermission(
                        PACKAGE_NAME,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    grantRuntimePermission(
                        PACKAGE_NAME,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
            }
        }
        baselineProfileRule.collect(PACKAGE_NAME) {
            startActivityAndWait()
            scrollMediaGrid()
            clickOnImage()
            waitMediaPagerAndPressBack()
            clickOnVideo()
            waitMediaPagerAndPressBack()
            navigateToBuckets()
            openBucket()
            waitMediaAndPressBack()
            navigateToImages()
        }
    }
}

private fun MacrobenchmarkScope.scrollMediaGrid() {
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

private fun MacrobenchmarkScope.navigateToImages() {
    device.waitForObject(By.res("nav_images")) { navItem ->
        navItem.click()
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.navigateToBuckets() {
    device.waitForObject(By.res("nav_buckets")) { navItem ->
        navItem.click()
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.openBucket() {
    device.waitForObject(By.res("bucket")) { bucketItem ->
        bucketItem.click()
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.waitMediaAndPressBack() {
    device.waitForObject(By.res(Pattern.compile("media_item_.*"))) {
        device.pressBack()
        device.waitForIdle()
    }
}

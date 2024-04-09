package com.github.yuriybudiyev.sketches.baselineprofile

import android.Manifest
import android.os.Build
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
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
import org.junit.runner.RunWith
import java.util.regex.Pattern
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        val packageName = (InstrumentationRegistry.getArguments()
            .getString("targetAppId")
            ?: throw Exception("targetAppId not passed as instrumentation runner arg"))
        with(InstrumentationRegistry.getInstrumentation().uiAutomation) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    grantRuntimePermission(
                        packageName,
                        Manifest.permission.READ_MEDIA_IMAGES
                    )
                    grantRuntimePermission(
                        packageName,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    grantRuntimePermission(
                        packageName,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
                else -> {
                    grantRuntimePermission(
                        packageName,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    grantRuntimePermission(
                        packageName,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
            }
        }
        rule.collect(
            packageName = packageName,
            includeInStartupProfile = true
        ) {
            pressHome()
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
        device.waitForObject(By.res("media_item_image"))
        device.waitForIdle()
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
        callsInPlace(
            block,
            InvocationKind.UNKNOWN
        )
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

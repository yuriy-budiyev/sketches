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

package com.github.yuriybudiyev.sketches.main

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.lifecycleScope
import com.github.yuriybudiyev.sketches.core.platform.share.LocalShareManager
import com.github.yuriybudiyev.sketches.core.platform.share.ShareManager
import com.github.yuriybudiyev.sketches.core.platform.systembars.LocalSystemBarsController
import com.github.yuriybudiyev.sketches.core.platform.systembars.SystemBarsController
import com.github.yuriybudiyev.sketches.core.ui.dimens.LocalDimens
import com.github.yuriybudiyev.sketches.core.ui.wsc.LocalWindowSizeClass
import com.github.yuriybudiyev.sketches.main.ui.MainScreen
import com.github.yuriybudiyev.sketches.main.ui.dimens.DefaultDimens
import com.github.yuriybudiyev.sketches.main.ui.theme.MainTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window!!
        val decorView = window.decorView
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, decorView).systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { view, windowInsets ->
            systemBarsController.isSystemBarsVisible =
                windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars()) ||
                    windowInsets.isVisible(WindowInsetsCompat.Type.statusBars())
            ViewCompat.onApplyWindowInsets(view, windowInsets)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.colorMode = ActivityInfo.COLOR_MODE_HDR
            window.desiredHdrHeadroom = 1.5F
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        }
        var contentReady by mutableStateOf(false)
        val contentView = findViewById<View>(android.R.id.content)!!
        contentView.viewTreeObserver.addOnPreDrawListener(
            object: ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (contentReady) {
                        contentView.viewTreeObserver.removeOnPreDrawListener(this)
                        return true
                    } else {
                        return false
                    }
                }
            },
        )
        var splashScreenExitCalled by mutableStateOf(false)
        lifecycleScope.launch {
            snapshotFlow { contentReady }.collect { contentReady ->
                if (contentReady) {
                    lifecycleScope.launch {
                        delay(timeMillis = 1000L)
                        if (!splashScreenExitCalled) {
                            contentView.alpha = 1F
                        }
                    }
                    cancel()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val darkMode =
                    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                        Configuration.UI_MODE_NIGHT_YES
                val insetsController = decorView.windowInsetsController!!
                if (darkMode) {
                    insetsController.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    )
                } else {
                    insetsController.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    )
                }
            }
            contentView.alpha = if (savedInstanceState != null) 1F else 0F
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                splashScreenExitCalled = true
                val animation = SpringAnimation(splashScreenView, SpringAnimation.ALPHA)
                animation.spring = SpringForce().apply {
                    stiffness = SpringForce.STIFFNESS_MEDIUM
                    dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                    finalPosition = 0F
                }
                animation.addEndListener { _, canceled, _, _ ->
                    if (!canceled) {
                        splashScreenView.remove()
                    }
                }
                lifecycleScope.launch {
                    delay(timeMillis = 200L)
                    contentView.alpha = 1F
                    animation.start()
                }
            }
        }
        setContent {
            @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
            CompositionLocalProvider(
                LocalDimens provides DefaultDimens(),
                LocalShareManager provides shareManager,
                LocalSystemBarsController provides systemBarsController,
                LocalWindowSizeClass provides calculateWindowSizeClass(this),
            ) {
                MainTheme {
                    MainScreen()
                }
            }
            LaunchedEffect(Unit) {
                contentReady = true
            }
        }
        ContextCompat.registerReceiver(
            this,
            shareReceiver,
            IntentFilter(ChooserCallbackResendAction),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onDestroy() {
        unregisterReceiver(shareReceiver)
        super.onDestroy()
    }

    override fun onMultiWindowModeChanged(
        isInMultiWindowMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        systemBarsController.isInMultiWindowMode = isInMultiWindowMode
    }

    private val onSharedListeners: MutableMap<String?, () -> Unit> = LinkedHashMap()

    private val shareReceiver: BroadcastReceiver = DynamicChooserCallbackReceiver()

    private val systemBarsController: SystemBarsControllerImpl =
        SystemBarsControllerImpl(
            isInMultiWindowMode = isInMultiWindowMode,
            isSystemBarsVisible = true,
        )

    private val shareManager: ShareManagerImpl = ShareManagerImpl()

    private companion object {

        const val ChooserCallbackResendAction: String =
            "com.github.yuriybudiyev.sketches.main.ChooserCallbackResendAction"

        const val ChooserCallbackActionExtra: String = "ChooserCallbackAction"
    }

    class ChooserCallbackReceiver: BroadcastReceiver() {

        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            context.sendBroadcast(
                Intent(ChooserCallbackResendAction)
                    .putExtra(
                        ChooserCallbackActionExtra,
                        intent.action,
                    )
                    .setPackage(context.packageName),
            )
        }
    }

    private inner class DynamicChooserCallbackReceiver: BroadcastReceiver() {

        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            lifecycleScope.launch {
                onSharedListeners[intent.getStringExtra(ChooserCallbackActionExtra)]?.invoke()
            }
        }
    }

    @Stable
    private inner class SystemBarsControllerImpl(
        isInMultiWindowMode: Boolean,
        isSystemBarsVisible: Boolean,
    ): SystemBarsController {

        override var isInMultiWindowMode: Boolean by mutableStateOf(isInMultiWindowMode)

        override var isSystemBarsVisible: Boolean by mutableStateOf(isSystemBarsVisible)

        override fun showSystemBars() {
            WindowCompat.getInsetsController(window, window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }

        override fun hideSystemBars() {
            WindowCompat.getInsetsController(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    @Stable
    private inner class ShareManagerImpl: ShareManager {

        override fun startChooserActivity(
            uri: Uri,
            mimeType: String,
            chooserTitle: CharSequence,
            listenerAction: String?,
        ) {
            val shareIntent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .setType(mimeType)
            if (listenerAction != null) {
                startChooserActivityWithCallback(
                    targetIntent = shareIntent,
                    chooserTitle = chooserTitle,
                    callbackAction = listenerAction,
                )
            } else {
                startChooserActivity(
                    targetIntent = shareIntent,
                    chooserTitle = chooserTitle,
                )
            }
        }

        override fun startChooserActivity(
            uris: ArrayList<Uri>,
            mimeType: String,
            chooserTitle: CharSequence,
            listenerAction: String?,
        ) {
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                .setType(mimeType)
            if (listenerAction != null) {
                startChooserActivityWithCallback(
                    targetIntent = shareIntent,
                    chooserTitle = chooserTitle,
                    callbackAction = listenerAction,
                )
            } else {
                startChooserActivity(
                    targetIntent = shareIntent,
                    chooserTitle = chooserTitle,
                )
            }
        }

        private fun startChooserActivity(
            targetIntent: Intent,
            chooserTitle: CharSequence,
        ) {
            val chooserIntent = Intent.createChooser(
                targetIntent,
                chooserTitle,
            )
            startActivity(chooserIntent)
        }

        private fun startChooserActivityWithCallback(
            targetIntent: Intent,
            chooserTitle: CharSequence,
            callbackAction: String,
        ) {
            val callbackIntent = PendingIntent.getBroadcast(
                applicationContext,
                callbackAction.hashCode(),
                Intent(
                    applicationContext,
                    ChooserCallbackReceiver::class.java,
                ).apply {
                    setAction(callbackAction)
                },
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val chooserIntent = Intent.createChooser(
                targetIntent,
                chooserTitle,
                callbackIntent.intentSender,
            )
            startActivity(chooserIntent)
        }

        override fun registerOnSharedListener(
            listenerAction: String,
            onShared: () -> Unit,
        ) {
            onSharedListeners[listenerAction] = onShared
        }

        override fun unregisterOnSharedListener(listenerAction: String) {
            onSharedListeners.remove(listenerAction)
        }
    }
}

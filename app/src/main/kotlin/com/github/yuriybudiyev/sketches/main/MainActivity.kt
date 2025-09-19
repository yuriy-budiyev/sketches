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
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.github.yuriybudiyev.sketches.core.platform.chooser.LocalShareManager
import com.github.yuriybudiyev.sketches.core.platform.chooser.ShareManager
import com.github.yuriybudiyev.sketches.core.ui.theme.SketchesTheme
import com.github.yuriybudiyev.sketches.main.ui.SketchesApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity: ComponentActivity(), ShareManager {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )
        val insetsController = WindowCompat.getInsetsController(
            window,
            window.decorView
        )
        val darkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            window.statusBarColor = Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                window.isStatusBarContrastEnforced = false
            }
            window.isNavigationBarContrastEnforced = false
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
            window.desiredHdrHeadroom = 1.5f
        }
        setContent {
            CompositionLocalProvider(LocalShareManager provides this) {
                SketchesTheme {
                    SketchesApp()
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            shareReceiver,
            IntentFilter(ACTION_SHARE_RESEND),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        unregisterReceiver(shareReceiver)
        super.onDestroy()
    }

    override fun startChooserActivity(
        uri: Uri,
        mimeType: String,
        title: CharSequence,
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND)
            .putExtra(
                Intent.EXTRA_STREAM,
                uri,
            )
            .setType(mimeType)
        val chooserIntent = Intent.createChooser(
            shareIntent,
            title,
        )
        startActivity(chooserIntent)
    }

    override fun startChooserActivity(
        content: ArrayList<Uri>,
        mimeType: String,
        title: CharSequence,
        listenerAction: String,
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
            .putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                content,
            )
            .setType(mimeType)
        val callbackIntent = PendingIntent.getBroadcast(
            applicationContext,
            listenerAction.hashCode(),
            Intent(
                applicationContext,
                ShareReceiver::class.java
            ).apply {
                setAction(listenerAction)
            },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val chooserIntent = Intent.createChooser(
            shareIntent,
            title,
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

    private val onSharedListeners: MutableMap<String?, () -> Unit> = LinkedHashMap()
    private val shareReceiver: BroadcastReceiver = DynamicShareReceiver()

    private companion object {

        const val ACTION_SHARE_RESEND = "com.github.yuriybudiyev.sketches.ACTION_SHARE_RESEND"
        const val EXTRA_SHARE_ACTION = "share_action"
    }

    class ShareReceiver: BroadcastReceiver() {

        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            context.sendBroadcast(
                Intent(ACTION_SHARE_RESEND)
                    .putExtra(
                        EXTRA_SHARE_ACTION,
                        intent.action
                    )
                    .setPackage(context.packageName)
            )
        }
    }

    private inner class DynamicShareReceiver: BroadcastReceiver() {

        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            lifecycleScope.launch {
                onSharedListeners[intent.getStringExtra(EXTRA_SHARE_ACTION)]?.invoke()
            }
        }
    }
}

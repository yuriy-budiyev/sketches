package com.github.yuriybudiyev.sketches.core.platform.chooser

import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf

interface ShareManager {

    fun startChooserActivity(
        uri: Uri,
        mimeType: String,
        title: CharSequence,
    )

    fun startChooserActivity(
        content: ArrayList<Uri>,
        mimeType: String,
        title: CharSequence,
        listenerAction: String,
    )

    fun addOnSharedListener(
        listenerAction: String,
        onShared: () -> Unit,
    )

    fun removeOnSharedListener(listenerAction: String)
}

val LocalShareManager = staticCompositionLocalOf<ShareManager> {
    error("No ShareProvider available")
}

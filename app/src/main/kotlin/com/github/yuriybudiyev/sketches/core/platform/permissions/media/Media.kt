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

package com.github.yuriybudiyev.sketches.core.platform.permissions.media

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.github.yuriybudiyev.sketches.core.platform.permissions.checkPermissionGranted

enum class MediaAccess {

    None,
    Full,
    UserSelected
}

fun Context.checkMediaAccess(): MediaAccess =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        if (checkPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES)
            && checkPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO)
        ) {
            MediaAccess.Full
        } else if (checkPermissionGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)) {
            MediaAccess.UserSelected
        } else {
            MediaAccess.None
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (checkPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES)
            && checkPermissionGranted(Manifest.permission.READ_MEDIA_VIDEO)
        ) {
            MediaAccess.Full
        } else {
            MediaAccess.None
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (checkPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            MediaAccess.Full
        } else {
            MediaAccess.None
        }
    } else {
        if (checkPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
            && checkPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            MediaAccess.Full
        } else {
            MediaAccess.None
        }
    }

@JvmInline
value class MediaAccessRequestLauncher(
    private val launcher: ActivityResultLauncher<Array<String>>,
) {

    fun requestMediaAccess() {
        launcher.launch(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        )
    }
}

@Composable
inline fun rememberMediaAccessRequestLauncher(
    crossinline onResult: (MediaAccess) -> Unit,
): MediaAccessRequestLauncher {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { grantResults ->
            val mediaAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (grantResults[Manifest.permission.READ_MEDIA_IMAGES] == true
                    && grantResults[Manifest.permission.READ_MEDIA_VIDEO] == true
                ) {
                    MediaAccess.Full
                } else if (grantResults[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true) {
                    MediaAccess.UserSelected
                } else {
                    MediaAccess.None
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (grantResults[Manifest.permission.READ_MEDIA_IMAGES] == true
                    && grantResults[Manifest.permission.READ_MEDIA_VIDEO] == true
                ) {
                    MediaAccess.Full
                } else {
                    MediaAccess.None
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (grantResults[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
                    MediaAccess.Full
                } else {
                    MediaAccess.None
                }
            } else {
                if (grantResults[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                    && grantResults[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
                ) {
                    MediaAccess.Full
                } else {
                    MediaAccess.None
                }
            }
            onResult(mediaAccess)
        },
    )
    return MediaAccessRequestLauncher(launcher)
}

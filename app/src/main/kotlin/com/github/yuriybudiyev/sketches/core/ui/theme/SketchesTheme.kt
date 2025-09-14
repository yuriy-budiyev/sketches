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

package com.github.yuriybudiyev.sketches.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import com.github.yuriybudiyev.sketches.core.ui.colors.SketchesColors
import com.github.yuriybudiyev.sketches.core.ui.typography.SketchesTypography

@Composable
@NonRestartableComposable
fun SketchesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            SketchesDarkColorScheme
        } else {
            SketchesLightColorScheme
        },
        typography = SketchesTypography,
        content = content
    )
}

private val SketchesLightColorScheme = lightColorScheme(
    primary = SketchesColors.Primary,
    onPrimary = SketchesColors.OnPrimary,
    secondary = SketchesColors.Light.Secondary,
    tertiary = SketchesColors.Light.Tertiary,
    background = SketchesColors.Light.Background,
    onBackground = SketchesColors.Light.OnBackground
)

private val SketchesDarkColorScheme = darkColorScheme(
    primary = SketchesColors.Primary,
    onPrimary = SketchesColors.OnPrimary,
    secondary = SketchesColors.Dark.Secondary,
    tertiary = SketchesColors.Dark.Tertiary,
    background = SketchesColors.Dark.Background,
    onBackground = SketchesColors.Dark.OnBackground
)

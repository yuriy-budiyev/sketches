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

package com.github.yuriybudiyev.sketches.main.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.yuriybudiyev.sketches.main.navigation.TopLevelDestination

@Composable
fun MainScreen() {
    val destinations = TopLevelDestination.entries
    var selectedIndex by remember { mutableIntStateOf(TopLevelDestination.IMAGES.ordinal) }
    NavigationBar {
        destinations.forEach { destination ->
            val selected = destination.ordinal == selectedIndex
            NavigationBarItem(selected = selected,
                onClick = { selectedIndex = destination.ordinal },
                icon = {
                    Icon(
                        imageVector = if (selected) {
                            destination.iconSelected
                        } else {
                            destination.iconUnselected
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = {
                    Text(text = stringResource(id = destination.labelRes))
                })
        }
    }
}

@Preview
@Composable
fun Preview() {
    MainScreen()
}
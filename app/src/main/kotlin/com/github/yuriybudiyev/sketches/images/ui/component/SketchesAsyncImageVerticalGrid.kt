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

package com.github.yuriybudiyev.sketches.images.ui.component

import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesAsyncImage
import com.github.yuriybudiyev.sketches.core.ui.component.SketchesLazyVerticalGrid
import com.github.yuriybudiyev.sketches.core.ui.icon.SketchesIcons
import com.github.yuriybudiyev.sketches.images.data.model.MediaStoreFile

typealias ImageClickListener = (index: Int, image: MediaStoreFile) -> Unit

@Composable
fun SketchesAsyncImageVerticalGrid(
    images: List<MediaStoreFile>,
    onImageClick: ImageClickListener
) {
    val data by rememberUpdatedState(images)
    val coroutineScope = rememberCoroutineScope()
    SketchesLazyVerticalGrid(modifier = Modifier.fillMaxSize()) {
        items(count = data.size,
            key = { index -> data[index].id },
            itemContent = { index ->
                val image = data[index]
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratio = 1f)
                    .clip(shape = RoundedCornerShape(8.dp))
                    .clickable {
                        coroutineScope.launch {
                            onImageClick(
                                index,
                                image
                            )
                        }
                    }) {
                    SketchesAsyncImage(
                        uri = image.uri,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    if (image.type == MediaStoreFile.Type.VIDEO) {
                        Icon(
                            imageVector = SketchesIcons.Video,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        )
                    }
                }
            })
    }
}

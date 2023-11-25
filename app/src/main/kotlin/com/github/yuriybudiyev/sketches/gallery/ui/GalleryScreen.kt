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

package com.github.yuriybudiyev.sketches.gallery.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import coil.compose.rememberAsyncImagePainter
import com.github.yuriybudiyev.sketches.gallery.data.model.GalleryImage

@Composable
fun GalleryScreen(viewModel: GalleryViewModel) {
    val uiState = viewModel.uiState.collectAsState()
    when (val state = uiState.value) {
        GalleryUiState.Empty -> {
            Text("Empty")
        }
        GalleryUiState.Loading -> {
            Text("Loading")
        }
        GalleryUiState.NoPermission -> {
            Text("No permission")
        }
        is GalleryUiState.Success -> {
            ImagesLazyGrid(state.images)
        }
        is GalleryUiState.Error -> {
            Text("Error")
        }
    }
}

@Composable
fun ImagesLazyGrid(images: List<GalleryImage>) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    LazyVerticalGrid(columns = GridCells.Fixed(if (landscape) 5 else 3)) {
        items(images,
              { it.id }) {
            val painter = rememberAsyncImagePainter(
                model = it.uri.toString()
            )
            Image(
                painter = painter,
                contentDescription = "Image",
                modifier = Modifier.aspectRatio(1.0f),
                contentScale = ContentScale.Crop
            )
        }
    }
}

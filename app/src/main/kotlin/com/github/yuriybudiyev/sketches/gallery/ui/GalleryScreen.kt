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

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.github.yuriybudiyev.sketches.R
import com.github.yuriybudiyev.sketches.gallery.data.model.GalleryImage

@Composable
fun GalleryScreen(viewModel: GalleryViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsState().value
    val imagesPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.updateImages()
        } else {
            viewModel.setNoPermission()
        }
    }
    val imagesPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    if (context.checkSelfPermission(imagesPermission) == PackageManager.PERMISSION_GRANTED) {
        if (uiState is GalleryUiState.Empty || uiState is GalleryUiState.NoPermission) {
            SideEffect {
                viewModel.updateImages()
            }
        }
    } else {
        SideEffect {
            viewModel.setNoPermission()
            imagesPermissionLauncher.launch(imagesPermission)
        }
    }
    when (uiState) {
        GalleryUiState.Empty -> {
            CenteredMessage(text = stringResource(id = R.string.gallery_ui_state_empty))
        }
        GalleryUiState.NoPermission -> {
            NoPermission()
        }
        GalleryUiState.Loading -> {
            Loading()
        }
        is GalleryUiState.Gallery -> {
            Gallery(images = uiState.images)
        }
        is GalleryUiState.Error -> {
            CenteredMessage(text = stringResource(id = R.string.gallery_ui_state_error))
        }
    }
}

@Composable
fun Gallery(images: List<GalleryImage>) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    LazyVerticalGrid(columns = GridCells.Fixed(if (landscape) 5 else 3)) {
        items(items = images,
              key = { it.id }) {
            AsyncImage(
                model = it.uri,
                contentDescription = "Image",
                modifier = Modifier.aspectRatio(ratio = 1.0f),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low
            )
        }
    }
}

@Composable
fun Message(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 18.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

@Composable
fun Loading() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(80.dp)
                .padding(16.dp)
        )
        Message(text = stringResource(id = R.string.gallery_ui_state_loading))
    }
}

@Composable
fun NoPermission() {
    val message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        stringResource(id = R.string.gallery_ui_state_no_permission_images)
    } else {
        stringResource(id = R.string.gallery_ui_state_no_permission_storage)
    }
    CenteredMessage(text = message)
}

@Composable
fun CenteredMessage(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Message(text = text)
    }
}

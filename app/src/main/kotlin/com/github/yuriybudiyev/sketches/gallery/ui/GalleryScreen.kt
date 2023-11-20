package com.github.yuriybudiyev.sketches.gallery.ui

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp

@Composable
fun GalleryScreen(viewModel: GalleryViewModel) {
    val uiState =
        viewModel.uiState.collectAsState()
    LazyVerticalGrid(columns = GridCells.Adaptive(128.dp)) {
        val images =
            uiState.value.images
        items(
            images,
            { it.id }) {

        }
    }
}

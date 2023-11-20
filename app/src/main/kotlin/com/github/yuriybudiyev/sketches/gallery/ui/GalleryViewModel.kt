package com.github.yuriybudiyev.sketches.gallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.gallery.model.reository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    val uiState: StateFlow<GalleryUiState>
        get() = uiStateInternal

    fun loadImages() {
        viewModelScope.launch {
            val images =
                withContext(Dispatchers.Default) { repository.getImages() }
            uiStateInternal.update { GalleryUiState(images ?: emptyList()) }
        }
    }

    private val repository: GalleryRepository =
        GalleryRepository(application.applicationContext)
    private val uiStateInternal: MutableStateFlow<GalleryUiState> =
        MutableStateFlow(GalleryUiState(emptyList()))
}

package com.github.yuriybudiyev.sketches.gallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.yuriybudiyev.sketches.gallery.model.data.GalleryImage
import com.github.yuriybudiyev.sketches.gallery.model.reository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryActivityViewModel(application: Application) : AndroidViewModel(application) {

    val images: LiveData<List<GalleryImage>>
        get() = imagesInternal

    fun loadImages() {
        viewModelScope.launch {
            val images = withContext(Dispatchers.Default) { repository.getImages() }
            imagesInternal.value = images ?: emptyList()
        }
    }

    private val repository: GalleryRepository = GalleryRepository(application.applicationContext)
    private val imagesInternal: MutableLiveData<List<GalleryImage>> = MutableLiveData()
}

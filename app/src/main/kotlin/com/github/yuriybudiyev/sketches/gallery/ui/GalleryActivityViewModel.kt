package com.github.yuriybudiyev.sketches.gallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.yuriybudiyev.sketches.gallery.model.reository.GalleryRepository

class GalleryActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GalleryRepository = GalleryRepository(application.applicationContext)
}

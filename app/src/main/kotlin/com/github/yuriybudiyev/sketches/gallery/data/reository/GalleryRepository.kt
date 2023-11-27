package com.github.yuriybudiyev.sketches.gallery.data.reository

import com.github.yuriybudiyev.sketches.gallery.data.model.GalleryImage

interface GalleryRepository {

    suspend fun getImages(): List<GalleryImage>?
}

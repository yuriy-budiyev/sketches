package com.github.yuriybudiyev.sketches.images.data.reository

import com.github.yuriybudiyev.sketches.images.data.model.MediaStoreImage

interface ImagesRepository {

    suspend fun getImages(): List<MediaStoreImage>?
}

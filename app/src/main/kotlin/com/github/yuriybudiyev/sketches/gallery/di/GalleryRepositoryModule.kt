package com.github.yuriybudiyev.sketches.gallery.di

import android.content.Context
import com.github.yuriybudiyev.sketches.gallery.data.reository.GalleryRepository
import com.github.yuriybudiyev.sketches.gallery.data.reository.implementation.GalleryRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GalleryRepositoryModule {

    @Provides
    @Singleton
    fun provideGalleryRepository(@ApplicationContext context: Context): GalleryRepository =
        GalleryRepositoryImpl(context)
}

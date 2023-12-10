package com.github.yuriybudiyev.sketches.images.di

import android.content.Context
import com.github.yuriybudiyev.sketches.images.data.reository.ImagesRepository
import com.github.yuriybudiyev.sketches.images.data.reository.implementation.ImagesRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImagesRepositoryModule {

    @Provides
    @Singleton
    fun provideGalleryRepository(@ApplicationContext context: Context): ImagesRepository =
        ImagesRepositoryImpl(context)
}

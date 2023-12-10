package com.github.yuriybudiyev.sketches.buckets.di

import android.content.Context
import com.github.yuriybudiyev.sketches.buckets.data.repository.BucketsRepository
import com.github.yuriybudiyev.sketches.buckets.data.repository.implementation.BucketsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface BucketsRepositoryModule {

    @Provides
    @Singleton
    fun provideBucketsRepository(@ApplicationContext context: Context): BucketsRepository =
        BucketsRepositoryImpl(context)
}

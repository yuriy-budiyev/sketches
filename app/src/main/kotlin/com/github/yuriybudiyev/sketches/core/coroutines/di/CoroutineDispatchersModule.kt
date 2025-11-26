package com.github.yuriybudiyev.sketches.core.coroutines.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
object CoroutineDispatchersModule {

    @Provides
    @Dispatcher(DispatcherType.Default)
    fun provideDefaultDispatcher(): CoroutineDispatcher =
        Dispatchers.Default

    @Provides
    @Dispatcher(DispatcherType.Main)
    fun provideMainDispatcher(): CoroutineDispatcher =
        Dispatchers.Main

    @Provides
    @Dispatcher(DispatcherType.IO)
    fun provideIODispatcher(): CoroutineDispatcher =
        Dispatchers.IO
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(@Suppress("unused") val type: DispatcherType)

enum class DispatcherType {
    Default,
    Main,
    IO,
}

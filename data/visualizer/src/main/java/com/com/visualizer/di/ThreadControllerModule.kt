package com.com.visualizer.di

import com.com.visualizer.data.ThreadLifecycleControllerImpl
import com.com.visualizer.domain.ThreadController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ThreadControllerModule {

	@Provides
	@Singleton
	fun providesThread(): ThreadController = ThreadLifecycleControllerImpl("ComputeThread")
}
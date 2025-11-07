package com.com.visualizer.di

import android.content.Context
import com.com.visualizer.data.AudioVisualizerImpl
import com.com.visualizer.domain.AudioVisualizer
import com.com.visualizer.domain.ThreadController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object VisualizerModule {

	@Provides
	@ViewModelScoped
	fun providesPlainVisualizer(
		@ApplicationContext context: Context,
		controller: ThreadController
	): AudioVisualizer = AudioVisualizerImpl(context, controller)
}
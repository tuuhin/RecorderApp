package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.recorder.RecorderFileProviderImpl
import com.eva.recorderapp.voice_recorder.data.recorder.RecorderActionHandlerImpl
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderActionHandler
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecorderSingletonModule {

	@Provides
	@Singleton
	fun providesRecorderFileProvider(
		@ApplicationContext context: Context
	): RecorderFileProvider = RecorderFileProviderImpl(context = context)


	@Provides
	@Singleton
	fun providesRecorderActionHandler(
		@ApplicationContext context: Context
	): RecorderActionHandler = RecorderActionHandlerImpl(context)
}
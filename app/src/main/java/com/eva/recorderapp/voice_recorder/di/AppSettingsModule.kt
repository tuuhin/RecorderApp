package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.datastore.RecorderSettingsRepoImpl
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderSettingsRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppSettingsModule {

	@Provides
	@Singleton
	fun providesRecorderSettings(
		@ApplicationContext context: Context
	): RecorderSettingsRepo = RecorderSettingsRepoImpl(context)
}
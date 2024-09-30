package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.datastore.RecorderAudioSettingsRepoImpl
import com.eva.recorderapp.voice_recorder.data.datastore.RecorderFileSettingsRepoImpl
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderFileSettingsRepo
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
		@ApplicationContext context: Context,
	): RecorderAudioSettingsRepo = RecorderAudioSettingsRepoImpl(context)

	@Provides
	@Singleton
	fun providesFileSettings(
		@ApplicationContext context: Context,
	): RecorderFileSettingsRepo = RecorderFileSettingsRepoImpl(context)


}
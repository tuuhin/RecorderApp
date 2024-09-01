package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.player.AudioAmplitudeReader
import com.eva.recorderapp.voice_recorder.data.player.MediaControllerProvider
import com.eva.recorderapp.voice_recorder.data.recordings.provider.PlayerFileProviderImpl
import com.eva.recorderapp.voice_recorder.domain.player.PlayerFileProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerRecordingsModule {

	@Provides
	@Singleton
	fun providesPlayerFileProvider(
		@ApplicationContext context: Context,
	): PlayerFileProvider = PlayerFileProviderImpl(context)

	@Provides
	@Singleton
	fun providesVisualizer(
		@ApplicationContext context: Context,
		fileProvider: PlayerFileProvider,
	): AudioAmplitudeReader = AudioAmplitudeReader(context, fileProvider)

	@Provides
	@Singleton
	fun providesMediaController(
		@ApplicationContext context: Context
	): MediaControllerProvider = MediaControllerProvider(context)
}
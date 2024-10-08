package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.location.LocationAddressProviderImpl
import com.eva.recorderapp.voice_recorder.data.player.AudioAmplitudeReader
import com.eva.recorderapp.voice_recorder.data.player.MediaControllerProvider
import com.eva.recorderapp.voice_recorder.data.recordings.provider.PlayerFileProviderImpl
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.location.LocationAddressProvider
import com.eva.recorderapp.voice_recorder.domain.player.PlayerFileProvider
import com.eva.recorderapp.voice_recorder.domain.player.WaveformsReader
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
	fun providesLocationAddressProvider(
		@ApplicationContext context: Context,
		settings: RecorderAudioSettingsRepo,
	): LocationAddressProvider = LocationAddressProviderImpl(
		context = context,
		settings = settings
	)


	@Provides
	@Singleton
	fun providesPlayerFileProvider(
		@ApplicationContext context: Context,
		locationAddressProvider: LocationAddressProvider,
	): PlayerFileProvider = PlayerFileProviderImpl(context, locationAddressProvider)

	@Provides
	@Singleton
	fun providesWaveformsReader(
		@ApplicationContext context: Context,
		fileProvider: PlayerFileProvider,
	): WaveformsReader = AudioAmplitudeReader(context, fileProvider)

	@Provides
	@Singleton
	fun providesMediaController(
		@ApplicationContext context: Context,
	): MediaControllerProvider = MediaControllerProvider(context)

}
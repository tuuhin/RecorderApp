package com.eva.player.di

import android.content.Context
import com.eva.player.data.reader.AudioAmplitudeReader
import com.eva.player.domain.WaveformsReader
import com.eva.recordings.domain.provider.PlayerFileProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AmplitudeReaderModule {

	@Provides
	@Singleton
	fun providesWaveformReader(
		@ApplicationContext context: Context,
		fileProvider: PlayerFileProvider,
	): WaveformsReader = AudioAmplitudeReader(context, fileProvider)
}
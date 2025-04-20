package com.eva.player.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.player.data.reader.AudioAmplitudeReader
import com.eva.player.domain.WaveformsReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@UnstableApi
@InstallIn(SingletonComponent::class)
object PlayerSingletonModule {

	@Provides
	@Singleton
	fun providesWaveformReader(
		@ApplicationContext context: Context,
	): WaveformsReader = AudioAmplitudeReader(context)

	@Provides
	@Singleton
	fun providesExoPlayer(
		@ApplicationContext context: Context,
		settings: RecorderAudioSettingsRepo,
	): Player {

		val attributes = AudioAttributes.Builder()
			.setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
			.setUsage(C.USAGE_MEDIA)
			.setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
			.build()

		val extractor = DefaultExtractorsFactory().apply {
			//set extractor flags later if there is some problem
			setAmrExtractorFlags(AmrExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
			setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
		}

		val mediaSourceFactory = DefaultMediaSourceFactory(context, extractor)

		return ExoPlayer.Builder(context)
			.setMediaSourceFactory(mediaSourceFactory)
			.setSkipSilenceEnabled(settings.audioSettings.skipSilences)
			.setAudioAttributes(attributes, true)
			.setTrackSelector(DefaultTrackSelector(context))
			.build()
	}
}
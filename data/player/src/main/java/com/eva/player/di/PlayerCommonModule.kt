package com.eva.player.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import com.eva.player.data.AudioMetadataRetrieverImpl
import com.eva.player.domain.AudioMetadataRetriever
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

typealias MediaSourceFactory = MediaSource.Factory

@Module
@InstallIn(SingletonComponent::class)
@OptIn(UnstableApi::class)
object PlayerCommonModule {

	@Provides
	@Singleton
	fun providesPlayerAudioAttributes(): AudioAttributes {
		return AudioAttributes.Builder()
			.setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
			.setUsage(C.USAGE_MEDIA)
			.setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
			.build()
	}

	@Provides
	@Singleton
	fun providesMediaExtractorFactory(@ApplicationContext context: Context): MediaSource.Factory {

		val extractor = DefaultExtractorsFactory().apply {
			//set extractor flags later if there is some problem
			setAmrExtractorFlags(AmrExtractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
			setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)
		}

		return DefaultMediaSourceFactory(context, extractor)
	}

	@Provides
	@Singleton
	fun providesAudioMetadataRetriever(
		@ApplicationContext context: Context,
		mediaSource: MediaSourceFactory
	): AudioMetadataRetriever = AudioMetadataRetrieverImpl(context, mediaSource)
}
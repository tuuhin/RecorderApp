package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.os.bundleOf
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
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.data.player.AudioPlayerMediaCallBacks
import com.eva.recorderapp.voice_recorder.data.service.AudioPlayerNotification
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@OptIn(UnstableApi::class)
@Module
@InstallIn(ServiceComponent::class)
object PlayerServiceModule {

	@Provides
	@ServiceScoped
	fun providesExoPlayer(
		@ApplicationContext context: Context,
		settings: RecorderAudioSettingsRepo,
	): Player {

		val attibutes = AudioAttributes.Builder()
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
			.setAudioAttributes(attibutes, true)
			.setTrackSelector(DefaultTrackSelector(context))
			.build()
	}

	@Provides
	@ServiceScoped
	fun providesPlayerNotification(
		@ApplicationContext context: Context
	): MediaNotification.Provider {
		return AudioPlayerNotification(context).apply {
			setSmallIcon(R.drawable.ic_record_player)
		}
	}

	@Provides
	@ServiceScoped
	fun providesServiceSessions(
		@ApplicationContext context: Context,
		player: Player,
	): MediaSession {

		val callback = AudioPlayerMediaCallBacks()

		val extras = bundleOf(
			MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_NEXT to true,
			MediaConstants.EXTRAS_KEY_SLOT_RESERVATION_SEEK_TO_PREV to true
		)

		return MediaSession.Builder(context, player)
			.setExtras(extras)
			.setCallback(callback)
			.build()
	}
}
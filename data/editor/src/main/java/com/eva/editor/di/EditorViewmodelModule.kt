package com.eva.editor.di

import android.content.Context
import androidx.annotation.OptIn
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
import com.eva.editor.data.AudioTrimmerImpl
import com.eva.editor.data.ClippablePlayerImpl
import com.eva.editor.domain.AudioTrimmer
import com.eva.editor.domain.SimpleAudioPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Named

@Module
@InstallIn(ViewModelComponent::class)
@OptIn(UnstableApi::class)
object EditorViewmodelModule {

	@Provides
	@ViewModelScoped
	@Named("EDITOR_PLAYER")
	fun providesExoPlayer(@ApplicationContext context: Context): Player {

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
			.setAudioAttributes(attributes, true)
			.setTrackSelector(DefaultTrackSelector(context))
			.build()
	}

	@Provides
	@ViewModelScoped
	fun providesAudioTrimmer(@ApplicationContext context: Context): AudioTrimmer =
		AudioTrimmerImpl(context)


	@Provides
	@ViewModelScoped
	fun providesEditorPlayer(@Named("EDITOR_PLAYER") player: Player): SimpleAudioPlayer =
		ClippablePlayerImpl(player)
}
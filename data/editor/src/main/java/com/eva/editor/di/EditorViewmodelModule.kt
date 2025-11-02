package com.eva.editor.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.eva.editor.data.EditableAudioPlayerImpl
import com.eva.editor.data.transformer.AudioTransformerImpl
import com.eva.editor.domain.AudioTransformer
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
	fun providesExoPlayer(
		@ApplicationContext context: Context,
		attributes: AudioAttributes,
		mediaSourceFactory: MediaSource.Factory
	): Player {
		return ExoPlayer.Builder(context)
			.setMediaSourceFactory(mediaSourceFactory)
			.setAudioAttributes(attributes, true)
			.setTrackSelector(DefaultTrackSelector(context))
			.setName("EDITOR_PLAYER")
			.build()
	}

	@Provides
	@ViewModelScoped
	fun providesAudioTrimmer(
		@ApplicationContext context: Context,
	): AudioTransformer = AudioTransformerImpl(context)

	@Provides
	@ViewModelScoped
	fun providesEditorPlayer(
		@Named("EDITOR_PLAYER") player: Player,
		mediaSourceFactory: MediaSource.Factory,
	): SimpleAudioPlayer = EditableAudioPlayerImpl(player, mediaSourceFactory)
}
package com.eva.editor.di

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.eva.editor.data.AudioTrimmerImpl
import com.eva.editor.data.ClippablePlayerImpl
import com.eva.editor.domain.AudioTrimmer
import com.eva.editor.domain.SimpleAudioPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@UnstableApi
@Module
@InstallIn(SingletonComponent::class)
class EditorModule {

	@Provides
	@Singleton
	fun providesAudioTrimmer(@ApplicationContext context: Context): AudioTrimmer =
		AudioTrimmerImpl(context)

	@Provides
	@Singleton
	fun providesEditorPlayer(player: Player): SimpleAudioPlayer = ClippablePlayerImpl(player)
}
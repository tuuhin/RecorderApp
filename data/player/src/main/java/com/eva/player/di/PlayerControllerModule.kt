package com.eva.player.di

import android.content.Context
import com.eva.player.data.player.MediaControllerProvider
import com.eva.player.domain.AudioFilePlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object PlayerControllerModule {

	@Provides
	@ViewModelScoped
	fun providesMediaControllerProvider(@ApplicationContext context: Context)
			: AudioFilePlayer = MediaControllerProvider(context)


}
package com.eva.player.di

import android.content.Context
import com.eva.player.data.MediaControllerProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object ControllerModule {

	@Provides
	@ViewModelScoped
	fun providesMediaControllerProvider(@ApplicationContext context: Context)
			: MediaControllerProvider = MediaControllerProvider(context)
}
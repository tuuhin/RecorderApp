package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.player.PlayerFileProviderImpl
import com.eva.recorderapp.voice_recorder.domain.player.PlayerFileProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecordingsPlayerModule {

	@Provides
	@Singleton
	fun providesPlayerFileProvider(
		@ApplicationContext context: Context
	): PlayerFileProvider = PlayerFileProviderImpl(context)
}
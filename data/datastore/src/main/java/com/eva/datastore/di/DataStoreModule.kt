package com.eva.datastore.di

import android.content.Context
import com.eva.datastore.data.repository.RecorderAudioSettingsRepoImpl
import com.eva.datastore.data.repository.RecorderFileSettingsRepoImpl
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.datastore.domain.repository.RecorderFileSettingsRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

	@Provides
	@Singleton
	fun providesRecorderSettings(
		@ApplicationContext context: Context,
	): RecorderAudioSettingsRepo = RecorderAudioSettingsRepoImpl(context)

	@Provides
	@Singleton
	fun providesFileSettings(
		@ApplicationContext context: Context,
	): RecorderFileSettingsRepo = RecorderFileSettingsRepoImpl(context)

}
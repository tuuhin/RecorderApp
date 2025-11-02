package com.eva.datastore.di

import com.eva.datastore.data.repository.PreferencesSettingsRepoImpl
import com.eva.datastore.data.repository.RecorderAudioSettingsRepoImpl
import com.eva.datastore.data.repository.RecorderFileSettingsRepoImpl
import com.eva.datastore.domain.DataStoreProvider
import com.eva.datastore.domain.repository.PreferencesSettingsRepo
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.datastore.domain.repository.RecorderFileSettingsRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

	@Provides
	@Singleton
	fun providesRecorderSettings(provider: DataStoreProvider): RecorderAudioSettingsRepo =
		RecorderAudioSettingsRepoImpl(provider.audioSettingsDataStore)

	@Provides
	@Singleton
	fun providesFileSettings(provider: DataStoreProvider): RecorderFileSettingsRepo =
		RecorderFileSettingsRepoImpl(provider.fileSettingsDataStore)

	@Provides
	@Singleton
	fun providesOnBoardingSettings(provider: DataStoreProvider): PreferencesSettingsRepo =
		PreferencesSettingsRepoImpl(provider.preferencesDataStore)

}
package com.eva.datastore.di

import com.eva.datastore.data.repository.PreferencesSettingsRepoImpl
import com.eva.datastore.data.repository.RecorderAudioSettingsRepoImpl
import com.eva.datastore.data.repository.RecorderFileSettingsRepoImpl
import com.eva.datastore.data.repository.RecordingsListPreferencesRepoImpl
import com.eva.datastore.data.repository.TranscriptionSettingsRepoImpl
import com.eva.datastore.domain.DataStoreProvider
import com.eva.datastore.domain.repository.PreferencesSettingsRepo
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.datastore.domain.repository.RecorderFileSettingsRepo
import com.eva.datastore.domain.repository.RecordingListPreferencesRepo
import com.eva.datastore.domain.repository.TranscriptionSettingsRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object RepositoryModule {

	@Provides
	@ViewModelScoped
	fun providesRecorderSettings(provider: DataStoreProvider): RecorderAudioSettingsRepo =
		RecorderAudioSettingsRepoImpl(provider.audioSettingsDataStore)

	@Provides
	@ViewModelScoped
	fun providesFileSettings(provider: DataStoreProvider): RecorderFileSettingsRepo =
		RecorderFileSettingsRepoImpl(provider.fileSettingsDataStore)

	@Provides
	@ViewModelScoped
	fun providesOnBoardingSettings(provider: DataStoreProvider): PreferencesSettingsRepo =
		PreferencesSettingsRepoImpl(provider.preferencesDataStore)

	@Provides
	@ViewModelScoped
	fun providesTranscriptionSettings(provider: DataStoreProvider): TranscriptionSettingsRepo =
		TranscriptionSettingsRepoImpl(provider.transcriptionsDataStore)

	@Provides
	@ViewModelScoped
	fun providesRecordingListSettings(provider: DataStoreProvider): RecordingListPreferencesRepo =
		RecordingsListPreferencesRepoImpl(provider.recordingsListSettingsDataStore)
}
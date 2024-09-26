package com.eva.recorderapp.voice_recorder.di

import com.eva.recorderapp.voice_recorder.domain.player.PlayerFileProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.use_cases.PlayerFileProviderFromIdUseCase
import com.eva.recorderapp.voice_recorder.domain.use_cases.RecordingsFromCategoriesUseCase
import com.eva.recorderapp.voice_recorder.domain.use_cases.RenameRecordingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object UseCasesModule {

	@Provides
	@ViewModelScoped
	fun providesRecordingWithCategoriesUseCase(
		recordings: VoiceRecordingsProvider,
		secondaryRecordingsData: RecordingsSecondaryDataProvider,
	): RecordingsFromCategoriesUseCase = RecordingsFromCategoriesUseCase(
		recordings = recordings,
		secondaryRecordings = secondaryRecordingsData
	)

	@Provides
	@ViewModelScoped
	fun providesRenameUseCase(
		recordings: VoiceRecordingsProvider,
	): RenameRecordingUseCase = RenameRecordingUseCase(recordings)


	@Provides
	@ViewModelScoped
	fun providesRecordingWithMetadataUseCase(
		fileProvider: PlayerFileProvider,
		secondaryRecordingsData: RecordingsSecondaryDataProvider,
	): PlayerFileProviderFromIdUseCase = PlayerFileProviderFromIdUseCase(
		playerFileProvider = fileProvider,
		metadataProvider = secondaryRecordingsData
	)
}
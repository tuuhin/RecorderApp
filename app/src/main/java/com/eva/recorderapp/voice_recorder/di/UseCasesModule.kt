package com.eva.recorderapp.voice_recorder.di

import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
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
	fun bindsRecordingsUseCase(
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
}
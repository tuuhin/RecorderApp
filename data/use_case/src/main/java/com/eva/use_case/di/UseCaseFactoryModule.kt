package com.eva.use_case.di

import com.eva.recordings.domain.provider.PlayerFileProvider
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.recordings.domain.provider.VoiceRecordingsProvider
import com.eva.recordings.domain.widgets.RecordingWidgetInteractor
import com.eva.use_case.usecases.GetRecordingsOfCurrentAppUseCase
import com.eva.use_case.usecases.PlayerFileProviderFromIdUseCase
import com.eva.use_case.usecases.RecordingsFromCategoriesUseCase
import com.eva.use_case.usecases.RenameRecordingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseFactoryModule {

	@Provides
	@ViewModelScoped
	fun providesRecordingsFromCategoriesUseCase(
		recordingsProvider: VoiceRecordingsProvider,
		secondaryDataProvider: RecordingsSecondaryDataProvider,
	): RecordingsFromCategoriesUseCase =
		RecordingsFromCategoriesUseCase(recordingsProvider, secondaryDataProvider)

	@Provides
	@ViewModelScoped
	fun providesRenameRecordingsUseCase(recordingsProvider: VoiceRecordingsProvider)
			: RenameRecordingUseCase = RenameRecordingUseCase(recordingsProvider)


	@Provides
	@ViewModelScoped
	fun providesPlayerFileProviderUseCase(
		playerFileProvider: PlayerFileProvider,
		secondaryDataProvider: RecordingsSecondaryDataProvider,
	): PlayerFileProviderFromIdUseCase =
		PlayerFileProviderFromIdUseCase(playerFileProvider, secondaryDataProvider)

	@Provides
	@ViewModelScoped
	fun providesGetCurrentAppRecordingsUseCase(
		recordingsProvider: VoiceRecordingsProvider,
		secondaryDataProvider: RecordingsSecondaryDataProvider,
		widgetInteractor: RecordingWidgetInteractor,
	): GetRecordingsOfCurrentAppUseCase =
		GetRecordingsOfCurrentAppUseCase(recordingsProvider, secondaryDataProvider, widgetInteractor)
}
package com.eva.use_case.di

import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.recordings.domain.provider.VoiceRecordingsProvider
import com.eva.recordings.domain.widgets.RecordingWidgetInteractor
import com.eva.use_case.usecases.GetRecordingsOfCurrentAppUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseSingletonModule {

	@Provides
	@Singleton
	fun providesGetCurrentAppRecordingsUseCase(
		recordings: VoiceRecordingsProvider,
		secondary: RecordingsSecondaryDataProvider,
		widgetInteractions: RecordingWidgetInteractor,
	): GetRecordingsOfCurrentAppUseCase =
		GetRecordingsOfCurrentAppUseCase(recordings, secondary, widgetInteractions)
}
package com.eva.feature_widget.di

import com.eva.use_case.usecases.GetRecordingsOfCurrentAppUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetsEntryPoint {

	// a custom entry point.
	fun providesCurrentAppRecordings(): GetRecordingsOfCurrentAppUseCase
}
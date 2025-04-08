package com.eva.recorder.di

import android.content.Context
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.location.domain.repository.LocationProvider
import com.eva.recorder.data.RecorderWidgetInteracterImpl
import com.eva.recorder.data.VoiceRecorderImpl
import com.eva.recorder.domain.RecorderWidgetInteractor
import com.eva.recorder.domain.VoiceRecorder
import com.eva.recordings.domain.provider.RecorderFileProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object RecorderServiceModule {

	@Provides
	@ServiceScoped
	fun providesVoiceRecorder(
		@ApplicationContext context: Context,
		fileProvider: RecorderFileProvider,
		settings: RecorderAudioSettingsRepo,
		locationProvider: LocationProvider,
	): VoiceRecorder = VoiceRecorderImpl(
		context = context,
		fileProvider = fileProvider,
		settings = settings,
		locationProvider = locationProvider
	)

	@Provides
	@ServiceScoped
	fun providesInteracter(@ApplicationContext context: Context): RecorderWidgetInteractor =
		RecorderWidgetInteracterImpl(context)
}
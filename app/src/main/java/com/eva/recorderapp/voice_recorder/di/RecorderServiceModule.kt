package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.recorder.VoiceRecorderImpl
import com.eva.recorderapp.voice_recorder.data.service.NotificationHelper
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderStopWatch
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
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
	fun providesStopWatch(): RecorderStopWatch = RecorderStopWatch()

	@Provides
	@ServiceScoped
	fun providesVoiceRecorder(
		@ApplicationContext context: Context,
		fileProvider: RecorderFileProvider,
		stopWatch: RecorderStopWatch
	): VoiceRecorder =
		VoiceRecorderImpl(context = context, fileProvider = fileProvider, stopWatch = stopWatch)

	@Provides
	@ServiceScoped
	fun providesNotificationHelper(
		@ApplicationContext context: Context
	): NotificationHelper = NotificationHelper(context = context)
}
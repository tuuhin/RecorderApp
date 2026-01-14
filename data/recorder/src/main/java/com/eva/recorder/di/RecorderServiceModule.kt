package com.eva.recorder.di

import android.content.Context
import com.eva.recorder.data.VoiceRecorderManager
import com.eva.recorder.data.service.NotificationHelper
import com.eva.recorder.domain.recorder.AudioVisualDataProvider
import com.eva.recorder.domain.recorder.TranscriptionProvider
import com.eva.recorder.domain.recorder.VoiceRecorder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
internal object RecorderServiceModule {

	@Provides
	@ServiceScoped
	fun providesVoiceRecorderManager(
		recorder: VoiceRecorder,
		visualizer: AudioVisualDataProvider,
		transcriber: TranscriptionProvider
	): VoiceRecorderManager = VoiceRecorderManager(recorder, visualizer, transcriber)

	@Provides
	@ServiceScoped
	fun providesNotificationHelper(@ApplicationContext context: Context): NotificationHelper =
		NotificationHelper(context)
}
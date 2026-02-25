package com.eva.recorder.di

import android.content.Context
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.datastore.domain.repository.TranscriptionSettingsRepo
import com.eva.location.domain.repository.LocationProvider
import com.eva.recorder.data.VoiceRecorderManager
import com.eva.recorder.data.recorder.AudioVisualizerDataProviderImpl
import com.eva.recorder.data.recorder.TranscriptionProviderImpl
import com.eva.recorder.data.recorder.VoiceRecorderImpl
import com.eva.recorder.data.service.NotificationHelper
import com.eva.recorder.domain.recorder.AudioByteDataProvider
import com.eva.recorder.domain.recorder.AudioDataReader
import com.eva.recorder.domain.recorder.AudioVisualDataProvider
import com.eva.recorder.domain.recorder.TranscriptionProvider
import com.eva.recorder.domain.recorder.VoiceRecorder
import com.eva.recorder.domain.stopwatch.RecorderStopWatch
import com.eva.recordings.domain.provider.RecorderFileProvider
import com.eva.transcribe.domain.AudioTranscriptor
import com.eva.utils.RecorderConstants
import dagger.Binds
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
	fun providesSingleVoiceRecorder(
		@ApplicationContext context: Context,
		stopWatch: RecorderStopWatch,
		fileProvider: RecorderFileProvider,
		settings: RecorderAudioSettingsRepo,
		locationProvider: LocationProvider,
		audioDataReader: AudioDataReader,
	): VoiceRecorderImpl = VoiceRecorderImpl(
		context = context,
		stopWatch = stopWatch,
		fileProvider = fileProvider,
		settings = settings,
		locationProvider = locationProvider,
		pcmReader = audioDataReader,
	)

	@Provides
	@ServiceScoped
	fun providesAudioVisuals(
		stopWatch: RecorderStopWatch,
		bytesSource: AudioByteDataProvider
	): AudioVisualDataProvider = AudioVisualizerDataProviderImpl(
		stopWatch = stopWatch,
		source = bytesSource,
		delayRate = RecorderConstants.AMPS_READ_DELAY_RATE,
		bufferSize = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
	)

	@Provides
	@ServiceScoped
	fun transcriptorProvider(
		source: AudioByteDataProvider,
		transcriptor: AudioTranscriptor,
		settingsRepo: TranscriptionSettingsRepo
	): TranscriptionProvider = TranscriptionProviderImpl(
		source = source,
		transcriptor = transcriptor,
		settingsRepo = settingsRepo
	)

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


	@Module
	@InstallIn(ServiceComponent::class)
	abstract class RecorderImplBindings {

		@Binds
		@ServiceScoped
		abstract fun providesAudioVoiceRecorder(impl: VoiceRecorderImpl): VoiceRecorder

		@Binds
		@ServiceScoped
		abstract fun providesAudioStreamSource(impl: VoiceRecorderImpl): AudioByteDataProvider
	}
}
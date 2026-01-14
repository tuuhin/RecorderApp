package com.eva.recorder.di

import android.content.Context
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.location.domain.repository.LocationProvider
import com.eva.recorder.data.RecorderWidgetInteracterImpl
import com.eva.recorder.data.recorder.AudioRecordAmplitudeReader
import com.eva.recorder.data.recorder.AudioVisualizerDataProviderImpl
import com.eva.recorder.data.recorder.TranscriptionProviderImpl
import com.eva.recorder.data.recorder.VoiceRecorderImpl
import com.eva.recorder.domain.RecorderWidgetInteractor
import com.eva.recorder.domain.recorder.AudioByteDataProvider
import com.eva.recorder.domain.recorder.AudioDataReader
import com.eva.recorder.domain.recorder.AudioVisualDataProvider
import com.eva.recorder.domain.recorder.TranscriptionProvider
import com.eva.recorder.domain.recorder.VoiceRecorder
import com.eva.recorder.domain.stopwatch.RecorderStopWatch
import com.eva.recordings.domain.provider.RecorderFileProvider
import com.eva.transcribe.domain.AudioTranscriptor
import com.eva.utils.RecorderConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RecorderSingletonModule {

	@Provides
	@Singleton
	fun providesAudioBytesReader(@ApplicationContext context: Context): AudioDataReader =
		AudioRecordAmplitudeReader(context)

	@Provides
	@Singleton
	fun providesStopWatch(): RecorderStopWatch =
		RecorderStopWatch(delayTime = RecorderConstants.AMPS_READ_DELAY_RATE)

	@Provides
	@Singleton
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
	@Singleton
	fun providesAudioVoiceRecorder(impl: VoiceRecorderImpl): VoiceRecorder = impl

	@Provides
	@Singleton
	fun providesAudioStreamSource(impl: VoiceRecorderImpl): AudioByteDataProvider = impl

	@Provides
	@Singleton
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
	@Singleton
	fun transcriptorProvider(
		source: AudioByteDataProvider,
		transcriptor: AudioTranscriptor
	): TranscriptionProvider = TranscriptionProviderImpl(source, transcriptor)


	@Provides
	@Singleton
	fun providesWidgetInteracter(@ApplicationContext context: Context): RecorderWidgetInteractor =
		RecorderWidgetInteracterImpl(context)
}
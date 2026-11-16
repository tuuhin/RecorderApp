package com.eva.recorder.di

import android.content.Context
import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.datastore.domain.repository.TranscriptionSettingsRepo
import com.eva.location.domain.repository.LocationProvider
import com.eva.recorder.data.VoiceRecorderManager
import com.eva.recorder.data.recorder.AndroidGenericMediaRecorder
import com.eva.recorder.data.recorder.AudioVisualizerDataProviderImpl
import com.eva.recorder.data.recorder.CustomLosslessMediaRecorder
import com.eva.recorder.data.recorder.TranscriptionProviderImpl
import com.eva.recorder.data.recorder.VoiceRecorderImpl
import com.eva.recorder.data.service.NotificationHelper
import com.eva.recorder.domain.GenericVoiceRecorder
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
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Named

@Module
@InstallIn(ServiceComponent::class)
internal object RecorderServiceModule {

    @Provides
    @ServiceScoped
    @Named("android_generic")
    fun providesLossyRecorder(
        @ApplicationContext context: Context,
        fileProvider: RecorderFileProvider,
        settings: RecorderAudioSettingsRepo,
        locationProvider: LocationProvider,
    ): GenericVoiceRecorder = AndroidGenericMediaRecorder(
        context = context,
        fileProvider = fileProvider,
        settings = settings,
        locationProvider = locationProvider,
    )

    @Provides
    @ServiceScoped
    @Named("custom_layer")
    fun providesLosslessRecorder(
        @ApplicationContext context: Context,
        fileProvider: RecorderFileProvider,
        settings: RecorderAudioSettingsRepo,
    ): GenericVoiceRecorder = CustomLosslessMediaRecorder(
        context = context,
        fileProvider = fileProvider,
        settings = settings,
    )


    @Provides
    @ServiceScoped
    fun providesSingleVoiceRecorder(
        stopWatch: RecorderStopWatch,
        fileProvider: RecorderFileProvider,
        settings: RecorderAudioSettingsRepo,
        audioDataReader: AudioDataReader,
        @Named("android_generic") androidRecorder: GenericVoiceRecorder,
        @Named("custom_layer") customRecorder: GenericVoiceRecorder,
    ): VoiceRecorderImpl = VoiceRecorderImpl(
        stopWatch = stopWatch,
        fileProvider = fileProvider,
        settings = settings,
        pcmReader = audioDataReader,
        androidRecorder = androidRecorder,
        customRecorder = customRecorder,
    )

    @Provides
    @ServiceScoped
    fun providesAudioVisuals(
        stopWatch: RecorderStopWatch,
        bytesSource: AudioByteDataProvider,
    ): AudioVisualDataProvider = AudioVisualizerDataProviderImpl(
        stopWatch = stopWatch,
        source = bytesSource,
        delayRate = RecorderConstants.AMPS_READ_DELAY_RATE,
        bufferSize = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE,
    )

    @Provides
    @ServiceScoped
    fun transcriptorProvider(
        source: AudioByteDataProvider,
        transcriptor: AudioTranscriptor,
        settingsRepo: TranscriptionSettingsRepo,
    ): TranscriptionProvider = TranscriptionProviderImpl(
        source = source,
        transcriptor = transcriptor,
        settingsRepo = settingsRepo,
    )

    @Provides
    @ServiceScoped
    fun providesVoiceRecorderManager(
        recorder: VoiceRecorder,
        visualizer: AudioVisualDataProvider,
        transcriber: TranscriptionProvider,
    ): VoiceRecorderManager = VoiceRecorderManager(recorder, visualizer, transcriber)

    @Provides
    @ServiceScoped
    fun providesNotificationHelper(@ApplicationContext context: Context): NotificationHelper =
        NotificationHelper(context)
}

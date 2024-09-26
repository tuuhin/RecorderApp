package com.eva.recorderapp.voice_recorder.di

import android.content.Context
import com.eva.recorderapp.voice_recorder.data.recorder.VoiceRecorderImpl
import com.eva.recorderapp.voice_recorder.data.service.NotificationHelper
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderFileProvider
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.domain.use_cases.BluetoothScoUseCase
import com.eva.recorderapp.voice_recorder.domain.use_cases.PhoneStateObserverUsecase
import com.eva.recorderapp.voice_recorder.domain.util.BluetoothScoConnect
import com.eva.recorderapp.voice_recorder.domain.util.PhoneStateObserver
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
	): VoiceRecorder = VoiceRecorderImpl(
		context = context,
		fileProvider = fileProvider,
		settings = settings
	)

	@Provides
	@ServiceScoped
	fun providesNotificationHelper(
		@ApplicationContext context: Context,
	): NotificationHelper = NotificationHelper(context = context)

	@Provides
	@ServiceScoped
	fun providesBluetoothConnectUseCase(
		settingsRepo: RecorderAudioSettingsRepo,
		scoConnect: BluetoothScoConnect,
	): BluetoothScoUseCase = BluetoothScoUseCase(
		settings = settingsRepo,
		bluetoothScoConnect = scoConnect
	)

	@Provides
	@ServiceScoped
	fun providesPauseRecordingOnCallUseCase(
		settingsRepo: RecorderAudioSettingsRepo,
		phoneStateObserver: PhoneStateObserver,
		voiceRecorder: VoiceRecorder,
	): PhoneStateObserverUsecase = PhoneStateObserverUsecase(
		settings = settingsRepo,
		observer = phoneStateObserver,
		voiceRecorder = voiceRecorder
	)
}
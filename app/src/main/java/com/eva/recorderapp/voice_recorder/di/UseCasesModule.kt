package com.eva.recorderapp.voice_recorder.di

import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.use_cases.BluetoothScoUseCase
import com.eva.recorderapp.voice_recorder.domain.use_cases.PauseRecordingOnCallUseCase
import com.eva.recorderapp.voice_recorder.domain.util.BluetoothScoConnect
import com.eva.recorderapp.voice_recorder.domain.util.PhoneStateObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCasesModule {

	@Provides
	@Singleton
	fun providesBluetoothConnectUseCase(
		settingsRepo: RecorderAudioSettingsRepo,
		scoConnect: BluetoothScoConnect,
	): BluetoothScoUseCase = BluetoothScoUseCase(
		settings = settingsRepo,
		bluetoothScoConnect = scoConnect
	)

	@Provides
	@Singleton
	fun providesPauseRecordingOnCallUseCase(
		settingsRepo: RecorderAudioSettingsRepo,
		phoneStateObserver: PhoneStateObserver,
	): PauseRecordingOnCallUseCase = PauseRecordingOnCallUseCase(
		settings = settingsRepo,
		observer = phoneStateObserver
	)
}
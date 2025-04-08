package com.eva.use_case.di

import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.interactions.domain.BluetoothScoConnect
import com.eva.interactions.domain.PhoneStateObserver
import com.eva.use_case.usecases.BluetoothScoUseCase
import com.eva.use_case.usecases.PhoneStateObserverUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object UseCaseServiceModule {

	@Provides
	@ServiceScoped
	fun providesScoUseCase(
		settings: RecorderAudioSettingsRepo,
		bluetoothScoConnect: BluetoothScoConnect,
	): BluetoothScoUseCase = BluetoothScoUseCase(settings, bluetoothScoConnect)

	@Provides
	@ServiceScoped
	fun providesPhoneStateObserverUseCase(
		settings: RecorderAudioSettingsRepo,
		phoneStateObserver: PhoneStateObserver,
	): PhoneStateObserverUseCase = PhoneStateObserverUseCase(settings, phoneStateObserver)
}
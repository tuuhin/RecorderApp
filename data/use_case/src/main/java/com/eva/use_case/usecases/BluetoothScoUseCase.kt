package com.eva.use_case.usecases

import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.interactions.domain.BluetoothScoConnect
import com.eva.interactions.domain.enums.BtSCOChannelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class BluetoothScoUseCase @Inject constructor(
	private val settings: RecorderAudioSettingsRepo,
	private val bluetoothScoConnect: BluetoothScoConnect,
) {
	private var _isScoAdded = MutableStateFlow(false)

	private val connectionMode: Flow<BtSCOChannelState>
		get() = bluetoothScoConnect.observeScoState


	suspend fun observeConnectedState(onStateConnected: () -> Unit) {
		connectionMode.collect { state ->
			if (state == BtSCOChannelState.CONNECTED) onStateConnected()
		}
	}


	suspend fun startConnectionIfAllowed() {
		val isPermitted = settings.audioSettings().useBluetoothMic
		if (isPermitted) {
			bluetoothScoConnect.beginScoConnection()
			_isScoAdded.update { true }
		}
	}


	fun closeConnectionIfPresent() {
		if (_isScoAdded.value) {
			bluetoothScoConnect.closeScoConnection()
			_isScoAdded.update { false }
		}
	}
}
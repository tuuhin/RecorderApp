package com.eva.recorderapp.voice_recorder.domain.use_cases

import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.util.BluetoothScoConnect
import com.eva.recorderapp.voice_recorder.domain.util.enums.BtSCOChannelState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class BluetoothScoUseCase(
	private val settings: RecorderAudioSettingsRepo,
	private val bluetoothScoConnect: BluetoothScoConnect,
) {
	private var _isScoAdded = MutableStateFlow(false)

	private val connectionMode: Flow<BtSCOChannelState>
		get() = bluetoothScoConnect.observeScoState


	fun observeConnectedState(scope: CoroutineScope, onStateConnected: () -> Unit) {
		connectionMode.onEach { state ->
			if (state == BtSCOChannelState.CONNECTED) {
				onStateConnected()
			}
		}.launchIn(scope)
	}


	fun startConnectionIfAllowed() {
		val isPermitted = settings.audioSettings.useBluetoothMic
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
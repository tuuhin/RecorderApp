package com.eva.use_case.usecases

import com.eva.datastore.domain.repository.RecorderAudioSettingsRepo
import com.eva.interactions.domain.PhoneStateObserver
import com.eva.interactions.domain.enums.PhoneState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class PhoneStateObserverUseCase(
	private val settings: RecorderAudioSettingsRepo,
	observer: PhoneStateObserver,
) {
	private val isPauseAllowed
		get() = settings.audioSettingsFlow.map { it.pauseRecordingOnCall }

	private val phoneStateObserver = observer.invoke()

	@OptIn(ExperimentalCoroutinesApi::class)
	suspend fun checkIfAllowedAndRinging(isRecording: Boolean, onPhoneRinging: () -> Unit) =
		isPauseAllowed
			.filter { it }
			.flatMapLatest {
				phoneStateObserver.map { phState -> phState == PhoneState.RINGING && isRecording }
			}
			.filter { it }
			.collectLatest { onPhoneRinging() }
}
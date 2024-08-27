package com.eva.recorderapp.voice_recorder.domain.use_cases

import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.util.PhoneStateObserver
import com.eva.recorderapp.voice_recorder.domain.util.enums.PhoneState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class PauseRecordingOnCallUseCase(
	private val settings: RecorderAudioSettingsRepo,
	private val observer: PhoneStateObserver,
) {
	private val isPauseAllowed = settings.audioSettingsFlow
		.map { it.pauseRecordingOnCall }

	val phoneStateObserver = observer.invoke()

	fun checkIfAllowedAndRinging(
		scope: CoroutineScope,
		onPhoneRinging: () -> Unit,
	) {
		combine(isPauseAllowed, phoneStateObserver) { isAllowed, state ->
			if (isAllowed && state == PhoneState.RINGING) {
				onPhoneRinging()
			}
		}.launchIn(scope)
	}
}
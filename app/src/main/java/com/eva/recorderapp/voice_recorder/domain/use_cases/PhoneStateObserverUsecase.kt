package com.eva.recorderapp.voice_recorder.domain.use_cases

import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.domain.util.PhoneStateObserver
import com.eva.recorderapp.voice_recorder.domain.util.enums.PhoneState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class PhoneStateObserverUsecase(
	private val settings: RecorderAudioSettingsRepo,
	private val observer: PhoneStateObserver,
	private val voiceRecorder: VoiceRecorder,
) {
	private val isPauseAllowed
		get() = settings.audioSettingsFlow.map { it.pauseRecordingOnCall }

	private val recorderState
		get() = voiceRecorder.recorderState

	val phoneStateObserver = observer.invoke()

	fun checkIfAllowedAndRinging(
		scope: CoroutineScope,
		onPhoneRinging: () -> Unit,
	) {
		combine(isPauseAllowed, phoneStateObserver, recorderState) { isAllowed, phState, rcState ->
			if (!isAllowed) return@combine
			if (rcState.isRecording && phState == PhoneState.RINGING) {
				onPhoneRinging()
			}
		}.launchIn(scope)
	}
}
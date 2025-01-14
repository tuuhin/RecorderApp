package com.eva.recorderapp.voice_recorder.domain.use_cases

import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.domain.util.PhoneStateObserver
import com.eva.recorderapp.voice_recorder.domain.util.enums.PhoneState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class PhoneStateObserverUseCase(
	private val settings: RecorderAudioSettingsRepo,
	observer: PhoneStateObserver,
	private val voiceRecorder: VoiceRecorder,
) {
	private val isPauseAllowed
		get() = settings.audioSettingsFlow.map { it.pauseRecordingOnCall }

	private val recorderState
		get() = voiceRecorder.recorderState

	private val phoneStateObserver = observer.invoke()

	@OptIn(ExperimentalCoroutinesApi::class)
	suspend fun checkIfAllowedAndRinging(onPhoneRinging: () -> Unit) = isPauseAllowed
		.filter { it }
		.flatMapLatest {
			combine(phoneStateObserver, recorderState) { phState, rcState ->
				rcState.isRecording && phState == PhoneState.RINGING
			}
		}
		.filter { it }
		.collectLatest { onPhoneRinging() }
}
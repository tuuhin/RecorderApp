package com.eva.recorderapp.voice_recorder.domain.util

import com.eva.recorderapp.voice_recorder.domain.util.enums.PhoneState
import kotlinx.coroutines.flow.Flow

fun interface PhoneStateObserver {

	/**
	 * Observe the current phone state ,ie. whether its ringing or in other state
	 * @see PhoneState
	 */
	operator fun invoke(): Flow<PhoneState>
}
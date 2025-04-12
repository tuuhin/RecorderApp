package com.eva.interactions.domain

import com.eva.interactions.domain.enums.PhoneState
import kotlinx.coroutines.flow.Flow


fun interface PhoneStateObserver {

	/**
	 * Observe the current phone state ,ie. whether its ringing or in other state
	 * @see PhoneState
	 */
	operator fun invoke(): Flow<PhoneState>
}
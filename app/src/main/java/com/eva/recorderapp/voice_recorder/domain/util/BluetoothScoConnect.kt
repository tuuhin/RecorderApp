package com.eva.recorderapp.voice_recorder.domain.util

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.util.enums.BtSCOChannelState
import com.eva.recorderapp.voice_recorder.domain.util.models.AudioDevice
import kotlinx.coroutines.flow.Flow

interface BluetoothScoConnect {

	val hasTelephonyFeature: Boolean

	val observeScoState: Flow<BtSCOChannelState>

	/**
	 * Observes the new communication device may return an empty flow if
	 * unable to evalutate the device
	 */
	val observeConnection: Flow<AudioDevice>

	/**
	 * Starts the synchornous connection orriented link (SCO's)
	 */
	fun beginScoConnection(): Resource<Boolean, Exception>

	/**
	 * Close this the sco link created
	 */
	fun closeScoConnection(): Resource<Unit, Exception>
}
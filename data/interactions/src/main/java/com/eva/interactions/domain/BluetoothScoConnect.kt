package com.eva.interactions.domain


import com.eva.interactions.domain.enums.BtSCOChannelState
import com.eva.interactions.domain.models.AudioDevice
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow

interface BluetoothScoConnect {

	val hasTelephonyFeature: Boolean

	val observeScoState: Flow<BtSCOChannelState>

	/**
	 * Observes the new communication device may return an empty flow if
	 * unable to evaluate the device
	 */
	val observeConnection: Flow<AudioDevice>

	/**
	 * Starts the synchronous connection oriented link (SCO's)
	 */
	fun beginScoConnection(): Resource<Boolean, Exception>

	/**
	 * Close this the sco link created
	 */
	fun closeScoConnection(): Resource<Unit, Exception>
}
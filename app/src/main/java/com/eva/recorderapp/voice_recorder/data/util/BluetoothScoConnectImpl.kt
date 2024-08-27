package com.eva.recorderapp.voice_recorder.data.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.util.BluetoothScoConnect
import com.eva.recorderapp.voice_recorder.domain.util.enums.BtSCOChannelState
import com.eva.recorderapp.voice_recorder.domain.util.exception.BluetoothScoAlreadyConnected
import com.eva.recorderapp.voice_recorder.domain.util.exception.BluetoothScoDeviceNotFound
import com.eva.recorderapp.voice_recorder.domain.util.exception.TelephonyFeatureNotException
import com.eva.recorderapp.voice_recorder.domain.util.models.AudioDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "BLUETOOTH_UTIL"

class BluetoothScoConnectImpl(
	private val context: Context
) : BluetoothScoConnect {


	private val audioManager by lazy { context.getSystemService<AudioManager>() }

	override val hasTelephonyFeature: Boolean
		get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

	override val observeConnection: Flow<AudioDevice>
		get() = callbackFlow {
			// TODO: Check the communication device
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				audioManager?.communicationDevice?.let { product ->
					trySend(product.toModel())
				}

				val listener = object : AudioManager.OnCommunicationDeviceChangedListener {
					override fun onCommunicationDeviceChanged(device: AudioDeviceInfo?) {
						device?.toModel()?.let { aud -> trySend(aud) }
					}
				}

				Log.d(TAG, "LISTENER FOR COMM DEVICE ADDED")
				audioManager?.addOnCommunicationDeviceChangedListener(
					Dispatchers.Main.asExecutor(),
					listener
				)

				awaitClose {
					Log.d(TAG, "LISTENER FOR COMM DEVICE REMOVED")
					audioManager?.removeOnCommunicationDeviceChangedListener(listener)
				}
			}
		}


	override val observeScoState: Flow<BtSCOChannelState>
		get() = callbackFlow {

			val receiver = BluetoothScoReceiver { state ->
				trySend(state)
				Log.d(TAG, "BT STATE ${state.name}")
			}

			val intentFilter = IntentFilter().apply {
				addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)

			}

			ContextCompat.registerReceiver(
				context,
				receiver,
				intentFilter,
				ContextCompat.RECEIVER_NOT_EXPORTED
			)
			Log.d(TAG, "RECIEVER REGISTERED")

			awaitClose {
				context.unregisterReceiver(receiver)
				Log.d(TAG, "RECEIVER UNREGISTERED")
			}
		}


	@Suppress("DEPRECATION")
	override fun beginScoConnection(): Resource<Boolean, Exception> {
		return try {
			if (audioManager?.isBluetoothScoAvailableOffCall == false) {
				Log.i(TAG, "SCO NOT AVAILABLE")
				return Resource.Error(TelephonyFeatureNotException())
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

				val connected = audioManager?.communicationDevice
					?: return Resource.Error(TelephonyFeatureNotException())

				if (connected.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO && connected.isSink) {
					Log.d(TAG, "DEVICE IS ALREDY CONNECTED :${connected.productName}")
					return Resource.Error(BluetoothScoAlreadyConnected())
				}

				val inputDevices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					val attributes = AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_MEDIA)
						.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
						.build()

					audioManager?.getAudioDevicesForAttributes(attributes)
				} else audioManager?.availableCommunicationDevices

				val filterDevices = inputDevices
					?.filter { it.isSink && it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }

				val device = filterDevices?.firstOrNull()
					?: return Resource.Error(BluetoothScoDeviceNotFound())

				// set the communication device
				val isOk = audioManager?.setCommunicationDevice(device)
				Log.d(TAG, "COMMUNICATON DEVICE SET :${device.productName} ")

				Resource.Success(isOk == true)

			} else {
				if (audioManager?.isBluetoothScoOn == true) {
					Log.i(TAG, "BLUETOOTH SCO IS ALREADY ON")
					return Resource.Error(BluetoothScoAlreadyConnected())
				}
				// start bluetooth sco
				val isOk = audioManager?.startBluetoothSco()
				Log.d(TAG, "START BLUETOOTH SCO ")
				Resource.Success(true)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

	@Suppress("DEPRECATION")
	override fun closeScoConnection(): Resource<Unit, Exception> {
		return try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				// clear the device connection
				audioManager?.clearCommunicationDevice()
				Log.d(TAG, "CLEAR COMMUNICATION DEVICE")
			} else {
				audioManager?.stopBluetoothSco()
				Log.d(TAG, "STOP BLUETOOTH SCO")
			}
			Resource.Success(Unit)
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

}

private class BluetoothScoReceiver(
	private val onNewState: (BtSCOChannelState) -> Unit
) : BroadcastReceiver() {

	override fun onReceive(context: Context?, intent: Intent?) {
		val state = intent?.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1)
			?: return

		val newState = when (state) {
			AudioManager.SCO_AUDIO_STATE_CONNECTED -> BtSCOChannelState.CONNECTED
			AudioManager.SCO_AUDIO_STATE_CONNECTING -> BtSCOChannelState.CONNECTING
			AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> BtSCOChannelState.DISCONNECTED
			AudioManager.SCO_AUDIO_STATE_ERROR -> BtSCOChannelState.ERROR
			else -> return
		}
		onNewState(newState)
	}
}


private fun AudioDeviceInfo.toModel() = AudioDevice(
	id = id,
	productName = productName?.toString()
)

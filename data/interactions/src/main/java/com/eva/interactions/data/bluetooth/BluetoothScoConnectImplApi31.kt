package com.eva.interactions.data.bluetooth

import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.eva.interactions.domain.BluetoothScoConnect
import com.eva.interactions.domain.enums.BtSCOChannelState
import com.eva.interactions.domain.exception.BluetoothScoAlreadyConnected
import com.eva.interactions.domain.exception.BluetoothScoDeviceNotFound
import com.eva.interactions.domain.exception.TelephonyFeatureNotException
import com.eva.interactions.domain.models.AudioDevice
import com.eva.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "BLUETOOTH_SCO_CONNECT"

@RequiresApi(Build.VERSION_CODES.S)
internal class BluetoothScoConnectImplApi31(private val context: Context) : BluetoothScoConnect {

	private val audioManager by lazy { context.getSystemService<AudioManager>() }

	override val hasTelephonyFeature: Boolean
		get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

	override val observeConnection: Flow<AudioDevice>
		get() = callbackFlow {

			audioManager?.communicationDevice?.let { product ->
				trySend(product.toModel())
			}

			val listener = AudioManager.OnCommunicationDeviceChangedListener { device ->
				device?.toModel()?.let { aud -> trySend(aud) }
			}

			Log.d(TAG, "LISTENER FOR COMM DEVICE ADDED")
			audioManager
				?.addOnCommunicationDeviceChangedListener(context.mainExecutor, listener)

			awaitClose {
				Log.d(TAG, "LISTENER FOR COMM DEVICE REMOVED")
				audioManager?.removeOnCommunicationDeviceChangedListener(listener)
			}
		}


	override val observeScoState: Flow<BtSCOChannelState>
		get() = callbackFlow {

			val receiver = BluetoothScoReceiver { state ->
				trySend(state)
				Log.d(TAG, "BT_SCO_STATE: ${state.name}")
			}

			ContextCompat.registerReceiver(
				context,
				receiver,
				IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED),
				ContextCompat.RECEIVER_NOT_EXPORTED
			)
			Log.d(TAG, "RECEIVER REGISTERED")

			awaitClose {
				context.unregisterReceiver(receiver)
				Log.d(TAG, "RECEIVER UNREGISTERED")
			}
		}


	override fun beginScoConnection(): Resource<Boolean, Exception> {
		return try {
			Log.i(TAG, "STARTING CONNECTION...")
			if (audioManager?.isBluetoothScoAvailableOffCall == false) {
				Log.i(TAG, "SCO NOT AVAILABLE")
				return Resource.Error(TelephonyFeatureNotException())
			}

			val connected = audioManager?.communicationDevice
				?: return Resource.Error(TelephonyFeatureNotException())

			Log.i(TAG, "DEVICE FOUND OF TYPE :${connected.type}")

			if (connected.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO && connected.isSink) {
				Log.d(TAG, "DEVICE IS ALREADY CONNECTED :${connected.productName}")
				return Resource.Error(BluetoothScoAlreadyConnected())
			}

			val filterDevices = audioManager?.availableCommunicationDevices
				?.filter { it.isSink && it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO }

			val device = filterDevices?.firstOrNull()
				?: return Resource.Error(BluetoothScoDeviceNotFound())

			// set the communication device
			val isOk = audioManager?.setCommunicationDevice(device)
			Log.d(TAG, "COMMUNICATION DEVICE SET :${device.productName} ")

			Resource.Success(isOk == true)

		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

	override fun closeScoConnection(): Resource<Unit, Exception> {
		return try {
			// clear the device connection
			audioManager?.clearCommunicationDevice()
			Log.d(TAG, "CLEAR COMMUNICATION DEVICE")
			Resource.Success(Unit)
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}
}
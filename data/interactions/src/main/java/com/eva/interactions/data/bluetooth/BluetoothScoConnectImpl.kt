package com.eva.interactions.data.bluetooth

import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.eva.interactions.domain.BluetoothScoConnect
import com.eva.interactions.domain.enums.BtSCOChannelState
import com.eva.interactions.domain.exception.BluetoothScoAlreadyConnected
import com.eva.interactions.domain.exception.TelephonyFeatureNotException
import com.eva.interactions.domain.models.AudioDevice
import com.eva.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

private const val TAG = "BLUETOOTH_SCO_CONNECT"

internal class BluetoothScoConnectImpl(private val context: Context) : BluetoothScoConnect {

	private val audioManager by lazy { context.getSystemService<AudioManager>() }

	override val hasTelephonyFeature: Boolean
		get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)


	/**
	 * In lower api we cannot observe the connection for [AudioDevice]
	 * @see BluetoothScoConnectImplApi31
	 */
	override val observeConnection: Flow<AudioDevice>
		get() = emptyFlow<AudioDevice>()

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


	@Suppress("DEPRECATION")
	override fun beginScoConnection(): Resource<Boolean, Exception> {
		return try {
			Log.i(TAG, "STARTING CONNECTION...")
			if (audioManager?.isBluetoothScoAvailableOffCall == false) {
				Log.i(TAG, "SCO NOT AVAILABLE")
				return Resource.Error(TelephonyFeatureNotException())
			}
			if (audioManager?.isBluetoothScoOn == true) {
				Log.i(TAG, "BLUETOOTH SCO IS ALREADY ON")
				return Resource.Error(BluetoothScoAlreadyConnected())
			}
			// start bluetooth sco
			audioManager?.startBluetoothSco()
			Log.d(TAG, "START BLUETOOTH SCO ")
			Resource.Success(true)

		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

	@Suppress("DEPRECATION")
	override fun closeScoConnection(): Resource<Unit, Exception> {
		return try {
			audioManager?.stopBluetoothSco()
			Log.d(TAG, "STOP BLUETOOTH SCO")
			Resource.Success(Unit)
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

}
package com.eva.interactions.data.bluetooth

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import com.eva.interactions.domain.BluetoothScoConnect
import com.eva.interactions.domain.enums.BtSCOChannelState
import com.eva.interactions.domain.exception.BluetoothSCONotAvailableException
import com.eva.interactions.domain.exception.BluetoothScoAlreadyConnected
import com.eva.interactions.domain.exception.BluetoothScoDeviceNotFound
import com.eva.interactions.domain.models.AudioDevice
import com.eva.utils.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

private const val TAG = "BLUETOOTH_SCO_CONNECT"

@RequiresApi(Build.VERSION_CODES.S)
internal class BluetoothScoConnectImplApi31(private val context: Context) : BluetoothScoConnect {

    private val audioManager by lazy { context.getSystemService<AudioManager>() }

    override val hasTelephonyFeature: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    private val commDeviceChange: Flow<AudioDeviceInfo?>
        get() = callbackFlow {
            trySend(audioManager?.communicationDevice)

            val listener = AudioManager.OnCommunicationDeviceChangedListener { device ->
                trySend(device)
            }

            Log.d(TAG, "LISTENER FOR COMM DEVICE ADDED")
            audioManager?.addOnCommunicationDeviceChangedListener(context.mainExecutor, listener)

            awaitClose {
                Log.d(TAG, "LISTENER FOR COMM DEVICE REMOVED")
                audioManager?.removeOnCommunicationDeviceChangedListener(listener)
            }
        }

    override val observeConnection: Flow<AudioDevice>
        get() = commDeviceChange.filterNotNull().map { it.toModel() }


    override val observeScoState: Flow<BtSCOChannelState>
        get() = commDeviceChange
            .map { device ->
                if (device?.type.isBluetoothAudioType()) BtSCOChannelState.CONNECTED
                else BtSCOChannelState.DISCONNECTED
            }
            .distinctUntilChanged()


    override fun beginScoConnection(): Resource<Boolean, Exception> {
        return try {
            Log.i(TAG, "STARTING CONNECTION...")
            val manager = audioManager
                ?: return Resource.Error(BluetoothSCONotAvailableException())

            val availableDevices = manager.availableCommunicationDevices
            if (availableDevices.isEmpty()) {
                Log.i(TAG, "NO COMMUNICATION DEVICES AVAILABLE")
                return Resource.Error(BluetoothSCONotAvailableException())
            }

            val connected = manager.communicationDevice
            if (connected != null && connected.isSink && connected.type.isBluetoothAudioType()) {
                Log.d(TAG, "DEVICE IS ALREADY CONNECTED :${connected.productName}")
                return Resource.Error(BluetoothScoAlreadyConnected())
            }

            val device = availableDevices.firstOrNull {
                it.isSink && it.type.isBluetoothAudioType()
            } ?: return Resource.Error(BluetoothScoDeviceNotFound())

            val isSet = manager.setCommunicationDevice(device)
            Log.d(TAG, "COMMUNICATION DEVICE SET :${device.productName} :$isSet")

            Resource.Success(isSet)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e)
        }
    }

    override fun closeScoConnection(): Resource<Unit, Exception> {
        return try {
            audioManager?.clearCommunicationDevice()
            Log.d(TAG, "CLEAR COMMUNICATION DEVICE")
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e)
        }
    }

    private fun Int?.isBluetoothAudioType(): Boolean =
        this == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || this == AudioDeviceInfo.TYPE_BLE_HEADSET
}
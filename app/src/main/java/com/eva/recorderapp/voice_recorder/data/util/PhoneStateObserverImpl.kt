@file:Suppress("DEPRECATION")

package com.eva.recorderapp.voice_recorder.data.util

import android.Manifest
import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.getSystemService
import com.eva.recorderapp.voice_recorder.domain.util.PhoneStateObserver
import com.eva.recorderapp.voice_recorder.domain.util.enums.PhoneState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "PHONE_STATE_OBSERVER"

class PhoneStateObserverImpl(
	private val context: Context
) : PhoneStateObserver {

	private val telephonyManager by lazy { context.getSystemService<TelephonyManager>() }

	private val hasPhoneStatePermission: Boolean
		get() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
				PermissionChecker.PERMISSION_GRANTED

	override fun invoke(): Flow<PhoneState> {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
			phoneStateListenerApi31()
		else phoneStateListener()
	}

	@RequiresApi(Build.VERSION_CODES.S)
	fun phoneStateListenerApi31() = callbackFlow<PhoneState> {

		if (!hasPhoneStatePermission) {
			Log.i(TAG, "PERMISSION WAS NOT GRANTED")
			return@callbackFlow awaitClose()
		}

		val listener = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
			override fun onCallStateChanged(state: Int) {
				val phoneState = when (state) {
					TelephonyManager.CALL_STATE_IDLE -> PhoneState.IDLE
					TelephonyManager.CALL_STATE_OFFHOOK -> PhoneState.OFF_HOOK
					TelephonyManager.CALL_STATE_RINGING -> PhoneState.RINGING
					else -> return
				}
				Log.d(TAG, "PHONE STATE :${phoneState.name}")
				trySend(phoneState)
			}
		}

		telephonyManager?.registerTelephonyCallback(Dispatchers.IO.asExecutor(), listener)
		Log.d(TAG, "PHONE STATE CALLBACK ADDED")

		awaitClose {
			Log.d(TAG, "PHONE STATE CALLBACK REOMVED")
			telephonyManager?.unregisterTelephonyCallback(listener)
		}
	}

	fun phoneStateListener() = callbackFlow<PhoneState> {

		if (!hasPhoneStatePermission) {
			Log.i(TAG, "PERMISSION WAS NOT GRANTED")
			return@callbackFlow awaitClose()
		}

		val listener = object : PhoneStateListener() {
			@Deprecated("Deprecated in Java")
			override fun onCallStateChanged(state: Int, phoneNumber: String?) {
				super.onCallStateChanged(state, phoneNumber)
				val phoneState = when (state) {
					TelephonyManager.CALL_STATE_IDLE -> PhoneState.IDLE
					TelephonyManager.CALL_STATE_OFFHOOK -> PhoneState.OFF_HOOK
					TelephonyManager.CALL_STATE_RINGING -> PhoneState.RINGING
					else -> return
				}
				trySend(phoneState)
				Log.d(TAG, "PHONE STATE :${phoneState.name}")
			}
		}

		telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
		Log.d(TAG, "PHONE STATE CALLBACK ADDED")

		awaitClose {
			Log.d(TAG, "PHONE STATE CALLBACK REOMVED")
			telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
		}
	}
}
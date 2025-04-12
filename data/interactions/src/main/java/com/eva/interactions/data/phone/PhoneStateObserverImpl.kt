@file:Suppress("DEPRECATION")

package com.eva.interactions.data.phone

import android.Manifest
import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.getSystemService
import com.eva.interactions.domain.PhoneStateObserver
import com.eva.interactions.domain.enums.PhoneState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "PHONE_STATE_OBSERVER"

internal class PhoneStateObserverImpl(private val context: Context) : PhoneStateObserver {

	private val telephonyManager by lazy { context.getSystemService<TelephonyManager>() }

	private val hasPhoneStatePermission: Boolean
		get() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
				PermissionChecker.PERMISSION_GRANTED

	override fun invoke(): Flow<PhoneState> {
		return callbackFlow {

			if (!hasPhoneStatePermission) {
				Log.i(TAG, "PERMISSION WAS NOT GRANTED")
				return@callbackFlow awaitClose()
			}

			val listener = object : PhoneStateListener() {

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
				Log.d(TAG, "PHONE STATE CALLBACK REMOVED")
				telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
			}
		}
	}

}
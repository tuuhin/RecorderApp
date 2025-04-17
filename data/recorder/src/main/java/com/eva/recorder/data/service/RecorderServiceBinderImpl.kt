package com.eva.recorder.data.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.eva.recorder.domain.RecorderServiceBinder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlin.time.Duration

private const val TAG = "RECORDER_SERVICE_BINDER"

@OptIn(ExperimentalCoroutinesApi::class)
internal class RecorderServiceBinderImpl(private val context: Context) : RecorderServiceBinder {

	private val _isBounded = MutableStateFlow(false)
	private var _service: VoiceRecorderService? = null

	override val recorderTimer = _isBounded.filter { it }
		.flatMapLatest { _service?.recorderTime ?: emptyFlow() }

	override val recorderState = _isBounded.filter { it }
		.flatMapLatest { _service?.recorderState ?: emptyFlow() }

	override val bookMarkTimes: Flow<Set<Duration>>
		get() = _isBounded.filter { it }
			.flatMapLatest { _service?.bookMarks ?: emptyFlow() }

	override val amplitudes = _isBounded.filter { it }
		.flatMapLatest { _service?.amplitudes ?: emptyFlow() }

	override val isConnectionReady: StateFlow<Boolean>
		get() = _isBounded

	private val serviceConnection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
			val binder = (service as? VoiceRecorderService.LocalBinder)
			_service = binder?.getService()
			_isBounded.update { true }
			Log.d(TAG, "SERVICE CONNECTED")
		}

		override fun onServiceDisconnected(name: ComponentName?) {
			_isBounded.update { false }
			_service = null
			Log.d(TAG, "SERVICE DISCONNECTED")
		}
	}


	override fun bindToService() {
		try {
			val intent = Intent(context, VoiceRecorderService::class.java)
			context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
			Log.d(TAG, "SERVICE BIND")
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun unBindService() {
		try {
			context.unbindService(serviceConnection)
			Log.d(TAG, "SERVICE UN-BIND")
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun cleanUp() {
		Log.d(TAG,"SERVICE BINDER CLEANUP")
		_service = null
		_isBounded.update { false }
	}
}
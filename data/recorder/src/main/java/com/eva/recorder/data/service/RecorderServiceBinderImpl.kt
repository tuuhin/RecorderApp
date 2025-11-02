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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlin.time.Duration

private const val TAG = "RECORDER_SERVICE_BINDER"

@OptIn(ExperimentalCoroutinesApi::class)
internal class RecorderServiceBinderImpl(private val context: Context) : RecorderServiceBinder {

	private val _isBounded = MutableStateFlow(false)
	private var _service = MutableStateFlow<VoiceRecorderService?>(null)

	private val _serviceInstanceFlow = combine(_isBounded, _service) { bounded, service ->
		if (bounded && service != null) service
		else null
	}.filterNotNull()

	override val recorderTimer = _serviceInstanceFlow.flatMapLatest { it.recorderTime }

	override val recorderState = _serviceInstanceFlow.flatMapLatest { it.recorderState }

	override val bookMarkTimes: Flow<Set<Duration>>
		get() = _serviceInstanceFlow.flatMapLatest { it.bookMarks }

	override val amplitudes = _serviceInstanceFlow.flatMapLatest { it.amplitudes }

	override val isConnectionReady: StateFlow<Boolean>
		get() = _isBounded

	private val serviceConnection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
			val binder = (service as? VoiceRecorderService.LocalBinder)
			_service.value = binder?.getService()
			val isBounded = _isBounded.updateAndGet { true }
			Log.d(TAG, "SERVICE CONNECTED :BOUNDED :$isBounded")
		}

		override fun onServiceDisconnected(name: ComponentName?) {
			val bounded = _isBounded.updateAndGet { false }
			_service.value = null
			Log.d(TAG, "SERVICE DISCONNECTED :BOUNDED:$bounded")
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
			val isBounded = _isBounded.updateAndGet { false }
			Log.d(TAG, "SERVICE UN-BIND BOUNDED:$isBounded")
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun cleanUp() {
		Log.d(TAG, "SERVICE BINDER CLEANUP")
		_service.value = null
		_isBounded.update { false }
	}
}
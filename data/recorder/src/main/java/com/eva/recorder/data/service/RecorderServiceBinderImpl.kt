package com.eva.recorder.data.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.eva.recorder.domain.RecorderServiceBinder
import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalTime

private const val TAG = "RECORDER_SERVICE_BINDER"

@OptIn(ExperimentalCoroutinesApi::class)
internal class RecorderServiceBinderImpl(private val context: Context) : RecorderServiceBinder {

    private val _lock = Any()

    @Volatile
    private var _isBounded = false

    private val _service = MutableStateFlow<RecorderDataInterface?>(null)

    override val isConnectionReady: StateFlow<Boolean>
        field = MutableStateFlow(false)

    override val recorderTimer: Flow<LocalTime> = serviceFlow(LocalTime(0, 0)) { it.recorderTime }

    override val recorderState: Flow<RecorderState> = serviceFlow(RecorderState.IDLE) { it.recorderState }

    override val bookMarkTimes: Flow<Set<Duration>> = serviceFlow(emptySet()) { it.bookMarks }

    override val amplitudes: Flow<List<RecordedPoint>> = serviceFlow(emptyList()) { it.amplitudes }

    override val transcriptions: Flow<String> = serviceFlow("") { it.transcript }

    @OptIn(FlowPreview::class)
    override fun recorderWarnings(): Flow<String> {
        return serviceFlow(null) { it.warnings }
            .debounce(100.milliseconds)
            .filterNotNull()
    }

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            synchronized(_lock) {
                // bound should be true for this method
                if (!_isBounded) {
                    Log.w(TAG, "BOUND FLAG IS INVALID UNBINDING THE SERVICE CONNECTION")
                    safeUnbind()
                    return
                }

                // connected and we get the iBinder data
                val dataInterface = service as? RecorderDataInterface
                if (dataInterface == null) {
                    Log.e(TAG, "BINDER IS NOT A DATA INTERFACE")
                    _service.value = null
                    isConnectionReady.value = false
                    return
                }
                _service.value = dataInterface
                isConnectionReady.value = true
                Log.i(TAG, "SERVICE CONNECTED")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.i(TAG, "SERVICE DISCONNECTED BINDING IS STILL PRESENT")
            // clear the old service connection and mark not connected
            _service.value = null
            isConnectionReady.value = false
        }

        override fun onBindingDied(name: ComponentName?) {
            Log.w(TAG, "SERVICE BINDING DIED")
            synchronized(_lock) { releaseBinding() }
        }

        override fun onNullBinding(name: ComponentName?) {
            Log.w(TAG, "SERVICE RETURNED NULL BINDING")
            synchronized(_lock) { releaseBinding() }
        }
    }

    override fun bindToService() = synchronized(_lock) {
        // if bound is already request
        if (_isBounded) return@synchronized

        val success = try {
            val intent = Intent(context, VoiceRecorderService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: SecurityException) {
            Log.e(TAG, "UNABLE TO BIND TO THE SERVICE", e)
            false
        }

        Log.d(TAG, "SERVICE BIND REQUEST SUCCESS: $success")
        // mark bound is requested
        if (success) _isBounded = true
    }

    override fun unBindService() = synchronized(_lock) {
        if (!_isBounded) return@synchronized
        releaseBinding()
    }

    override fun cleanUp() {
        Log.d(TAG, "SERVICE BINDER CLEANUP")
        unBindService()
    }

    private fun releaseBinding() {
        safeUnbind()
        _isBounded = false
        _service.value = null
        isConnectionReady.value = false
        Log.d(TAG, "SERVICE UNBOUNDED")
    }

    private fun safeUnbind() {
        try {
            Log.i(TAG, "SERVICE UNBIND CALLED")
            context.unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "CONNECTION NOT REGISTERED", e)
        }
    }

    private fun <T> serviceFlow(default: T, selector: (RecorderDataInterface) -> Flow<T>): Flow<T> =
        _service.flatMapLatest { service -> if (service != null) selector(service) else flowOf(default) }

}

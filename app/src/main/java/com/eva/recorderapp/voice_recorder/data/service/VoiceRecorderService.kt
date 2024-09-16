package com.eva.recorderapp.voice_recorder.data.service

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.eva.recorderapp.R
import com.eva.recorderapp.common.NotificationConstants
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.use_cases.BluetoothScoUseCase
import com.eva.recorderapp.voice_recorder.domain.use_cases.PhoneStateObserverUsecase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val LOGGER_TAG = "RECORDER_SERVICE"

@AndroidEntryPoint
class VoiceRecorderService : LifecycleService() {

	@Inject
	lateinit var voiceRecorder: VoiceRecorder

	@Inject
	lateinit var notificationHelper: NotificationHelper

	@Inject
	lateinit var bluetoothScoUseCase: BluetoothScoUseCase

	@Inject
	lateinit var phoneStateObserverUseCase: PhoneStateObserverUsecase

	private val binder = LocalBinder()

	private val _amplitudes = MutableStateFlow(persistentListOf<Float>())

	val amplitudes: StateFlow<ImmutableList<Float>>
		get() = _amplitudes.asStateFlow()

	val recorderTime: StateFlow<LocalTime>
		get() = voiceRecorder.recorderTimer

	val recorderState: StateFlow<RecorderState>
		get() = voiceRecorder.recorderState

	@OptIn(FlowPreview::class)
	private val notificationTimer: Flow<LocalTime>
		get() = voiceRecorder.recorderTimer.sample(800.milliseconds)

	inner class LocalBinder : Binder() {

		fun getService(): VoiceRecorderService =
			this@VoiceRecorderService
	}

	override fun onBind(intent: Intent): IBinder {
		super.onBind(intent)
		Log.d(LOGGER_TAG, "SERVICE BOUNDED")
		return binder
	}

	override fun onCreate() {
		super.onCreate()
		try {
			// read phone states
			// preparing the recorder
			voiceRecorder.createRecorder()
			// check use case
			bluetoothScoUseCase.startConnectionIfAllowed()
			// listen to changes
			observeChangingPhoneState()
			// inform bt connect
			showBluetoothConnectedToast()
			// updates the amplitudes
			readAmplitudes()
			// update the notification
			readTimerAndUpdateNotification()

			Log.i(LOGGER_TAG, "SERVICE CREATED WITH OBSERVERS")
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		when (intent?.action) {
			RecorderAction.START_RECORDER.action -> onStartRecording()
			RecorderAction.RESUME_RECORDER.action -> onResumeRecording()
			RecorderAction.PAUSE_RECORDER.action -> onPauseRecording()
			RecorderAction.STOP_RECORDER.action -> onStopRecording()
			RecorderAction.CANCEL_RECORDER.action -> onCancelRecording()
		}
		return super.onStartCommand(intent, flags, startId)
	}


	private fun readAmplitudes() {
		voiceRecorder.maxAmplitudes.onEach { array ->
			// if array is empty then an empty list
			// otherwise the converted one
			val immutableList = if (array.isEmpty()) persistentListOf()
			else array.toList().toPersistentList()

			_amplitudes.update { immutableList }
		}.flowOn(Dispatchers.Default)
			.launchIn(lifecycleScope)
	}


	private fun showBluetoothConnectedToast() {
		bluetoothScoUseCase.observeConnectedState(
			scope = lifecycleScope,
			onStateConnected = ::showScoConnectToast
		)
	}

	private fun observeChangingPhoneState() {
		phoneStateObserverUseCase.checkIfAllowedAndRinging(
			scope = lifecycleScope,
			onPhoneRinging = {
				phoneRingingToast()
				onPauseRecording()
			},
		)
	}


	private fun readTimerAndUpdateNotification() {
		combine(notificationTimer, recorderState) { time, state ->
			when (state) {
				RecorderState.RECORDING -> notificationHelper.showNotificationDuringRecording(time)
				RecorderState.COMPLETED -> notificationHelper.setRecordingsCompletedNotification()
				RecorderState.CANCELLED -> notificationHelper.setRecordingCancelNotification()
				else -> {}
			}
		}.launchIn(lifecycleScope)
	}

	private fun onStartRecording() {
		//start the recorder
		lifecycleScope.launch { voiceRecorder.startRecording() }
			.invokeOnCompletion {
				// start foreground service
				startVoiceRecorderService(
					NotificationConstants.RECORDER_NOTIFICATION_ID,
					notificationHelper.timerNotification
				)
			}
	}

	override fun onUnbind(intent: Intent?): Boolean {
		// just a log.
		Log.d(LOGGER_TAG, "SERVICE UN_BOUNDED")
		return super.onUnbind(intent)
	}

	private fun onResumeRecording() {
		//update the notification
		notificationHelper.setOnResumeNotification()
		//resume recording
		voiceRecorder.resumeRecording()
	}

	private fun onPauseRecording() {
		//update the notification
		notificationHelper.setOnPauseNotification()
		//pause recording
		voiceRecorder.pauseRecording()
	}

	private fun onCancelRecording() {
		// cancel recording
		lifecycleScope.launch { voiceRecorder.cancelRecording() }
			.invokeOnCompletion {
				// stop the foreground
				stopForeground(STOP_FOREGROUND_REMOVE)
				// stop the service
				stopSelf()
			}
	}

	private fun onStopRecording() {
		// stop the recording
		lifecycleScope.launch { voiceRecorder.stopRecording() }
			.invokeOnCompletion {
				// stop the foreground
				stopForeground(STOP_FOREGROUND_REMOVE)
				// stop the service
				stopSelf()
			}
	}

	override fun onDestroy() {
		// close sco connection
		bluetoothScoUseCase.closeConnectionIfPresent()

		// resources are cleared
		voiceRecorder.releaseResources()
		Log.i(LOGGER_TAG, "RECORDER SERVICE DESTROYED")
		super.onDestroy()
	}
}

private fun Context.phoneRingingToast() =
	Toast.makeText(applicationContext, R.string.toast_incoming_call, Toast.LENGTH_SHORT)
		.show()

private fun Context.showScoConnectToast() =
	Toast.makeText(applicationContext, R.string.toast_recording_bluetooth, Toast.LENGTH_LONG)
		.show()

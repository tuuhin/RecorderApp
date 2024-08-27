package com.eva.recorderapp.voice_recorder.data.service

import android.app.Service
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
import com.eva.recorderapp.voice_recorder.domain.datastore.repository.RecorderAudioSettingsRepo
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.use_cases.BluetoothScoUseCase
import com.eva.recorderapp.voice_recorder.domain.use_cases.PauseRecordingOnCallUseCase
import com.eva.recorderapp.voice_recorder.domain.util.enums.BtSCOChannelState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime
import javax.inject.Inject

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
	lateinit var phoneStateObserverUseCase: PauseRecordingOnCallUseCase

	@Inject
	lateinit var recorderSettings: RecorderAudioSettingsRepo

	private val binder = LocalBinder()

	private val _amplitudes = MutableStateFlow(persistentListOf<Float>())

	val amplitides: StateFlow<ImmutableList<Float>>
		get() = _amplitudes.asStateFlow()

	val recorderTime: StateFlow<LocalTime>
		get() = voiceRecorder.recorderTimer

	val recorderState: StateFlow<RecorderState>
		get() = voiceRecorder.recorderState

	private val notificationTimer: Flow<LocalTime>
		get() = voiceRecorder.recorderTimer.buffer(1)
			.map { time -> LocalTime.fromSecondOfDay(time.toSecondOfDay()) }
			.distinctUntilChanged()
			.flowOn(Dispatchers.Unconfined)


	inner class LocalBinder : Binder() {

		fun getService(): VoiceRecorderService =
			this@VoiceRecorderService
	}

	override fun onBind(intent: Intent): IBinder? {
		super.onBind(intent)
		Log.d(LOGGER_TAG, "SERVICE BINDED")
		return binder
	}

	override fun onCreate() {
		super.onCreate()
		try {
			// read phone states
			// preparing the recorder
			voiceRecorder.createRecorder()
			// check usecase
			bluetoothScoUseCase.startConnectionIfAllowed()
			// listen to changes
			observeChangingPhoneState()
			// inform bt connect
			showBluetoothConnectedToast()
			// updates the amplitudes
			readAmplitudes()
			// update the notification
			readTimerAndUpdateNotification()
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
		Log.d(LOGGER_TAG, "READING AMPLITUDES")

		voiceRecorder.maxAmplitudes.onEach { array ->
			val immutableList = withContext(Dispatchers.Default) {
				if (array.isEmpty()) return@withContext persistentListOf()
				array.toList().toPersistentList()
			}
			_amplitudes.update { immutableList }
		}.launchIn(lifecycleScope)
	}


	private fun showBluetoothConnectedToast() = bluetoothScoUseCase.connectionMode
		.onEach { state ->
			if (state == BtSCOChannelState.CONNECTED) {
				applicationContext.showScoConnectToast()
			}
		}.launchIn(lifecycleScope)


	private fun observeChangingPhoneState() {
		phoneStateObserverUseCase.checkIfAllowedAndRinging(
			scope = lifecycleScope,
			onPhoneRinging = {
				applicationContext.phoneRingingToast()
				onPauseRecording()
			},
		)
	}


	private fun readTimerAndUpdateNotification() {
		Log.d(LOGGER_TAG, "READING TIMER AND UPDATING NOTIFICAITONS")

		combine(notificationTimer, recorderState) { time, state ->
			when (state) {
				RecorderState.RECORDING -> notificationHelper.showNotificationDuringRecording(time)
				RecorderState.COMPLETED -> notificationHelper.setRecordingsCompletedNotifcation()
				RecorderState.CANCELLED -> notificationHelper.setRecordingCancelNotificaion()
				else -> {}
			}
		}.launchIn(lifecycleScope)
	}

	private fun onStartRecording() {
		//start the recorder
		lifecycleScope.launch { voiceRecorder.startRecording() }
		// start foreground service
		startForegroundServiceMicrophone(
			NotificationConstants.RECORDER_NOTIFICATION_ID,
			notificationHelper.timerNotification
		)
	}


	private fun onResumeRecording() {
		//update the notification
		notificationHelper.setOnResumeNotification()
		//resume recording
		voiceRecorder.resumeRecording()
	}

	private fun onPauseRecording() {
		//update the notification
		notificationHelper.setOnPauseNotifcation()
		//pause recording
		voiceRecorder.pauseRecording()
	}

	private fun onCancelRecording() {
		// cancel recording
		lifecycleScope.launch { voiceRecorder.cancelRecording() }
			.invokeOnCompletion {
				// stop the foregound
				stopForeground(Service.STOP_FOREGROUND_DETACH)
			}
	}

	private fun onStopRecording() {
		// stop the recording
		lifecycleScope.launch { voiceRecorder.stopRecording() }
			.invokeOnCompletion {
				// stop the foreground
				stopForeground(Service.STOP_FOREGROUND_DETACH)
			}
	}

	override fun onDestroy() {
		// close sco connection
		bluetoothScoUseCase.closeConnectionIfPresent()
		// resources are cleared
		voiceRecorder.releaseResources()
		Log.d(LOGGER_TAG, "SERVICE DESTROYED")
		super.onDestroy()
	}
}

private fun Context.phoneRingingToast() =
	Toast.makeText(this, R.string.toast_incoming_call, Toast.LENGTH_SHORT)
		.show()

private fun Context.showScoConnectToast() =
	Toast.makeText(this, R.string.toast_recording_bluetooth, Toast.LENGTH_LONG)
		.show()

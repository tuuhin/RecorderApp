package com.eva.recorderapp.voice_recorder.data.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.eva.recorderapp.R
import com.eva.recorderapp.common.LocalTimeFormats
import com.eva.recorderapp.common.NotificationConstants
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import javax.inject.Inject

private const val LOGGER_TAG = "RECORDER_SERVICE"

@AndroidEntryPoint
class VoiceRecorderService : LifecycleService() {

	@Inject
	lateinit var voiceRecorder: VoiceRecorder

	@Inject
	lateinit var notificationHelper: NotificationHelper

	private val _notificationManager by lazy { getSystemService<NotificationManager>() }

	private val binder = LocalBinder()

	private val _amplitudes = MutableStateFlow(floatArrayOf())

	@OptIn(ExperimentalCoroutinesApi::class)
	val amplitides: StateFlow<ImmutableList<Float>>
		get() = _amplitudes.mapLatest { array ->
			if (array.isEmpty()) return@mapLatest persistentListOf()
			val listbuilder = persistentListOf<Float>().builder()
			array.forEach(listbuilder::add)
			listbuilder.build()
		}.stateIn(
			scope = lifecycleScope,
			started = SharingStarted.Lazily,
			initialValue = persistentListOf()
		)

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
		// preparing the recorder
		voiceRecorder.createRecorder()
		// listen to changes
		try {
			// updates the amplitudes
			readAmplitudes()
			// update the notification
			readTimerAndUpdateNotification()
		}  catch (e: Exception) {
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

		combine(voiceRecorder.maxAmplitudes, voiceRecorder.recorderState) { array, state ->
			when (state) {
				// in recording or paused state we update the recordings accordingly
				RecorderState.RECORDING, RecorderState.PAUSED -> _amplitudes.update { array }
				// on completed amplitides should reset again
				else -> _amplitudes.update { floatArrayOf() }
			}
		}.launchIn(lifecycleScope)
	}


	private fun readTimerAndUpdateNotification() {

		Log.d(LOGGER_TAG, "UPDATING NOTIFICATION STATE")

		combine(notificationTimer, recorderState) { time, state ->
			when (state) {
				RecorderState.RECORDING -> {
					val readableTime = time.format(LocalTimeFormats.NOTIFICATION_TIMER_TIME_FORMAT)
					// set the title
					notificationHelper.setContentTitle(readableTime)
					// show the notification
					_notificationManager?.notify(
						NotificationConstants.RECORDER_NOTIFICATION_ID,
						notificationHelper.timerNotification
					)
				}

				RecorderState.COMPLETED -> {
					_notificationManager?.notify(
						NotificationConstants.RECORDER_NOTIFICATION_ID,
						notificationHelper.recordingCompleteNotification
					)
				}

				RecorderState.CANCELLED -> {
					_notificationManager?.notify(
						NotificationConstants.RECORDER_NOTIFICATION_ID,
						notificationHelper.recordingCancelNotificaiton
					)
				}

				else -> {}
			}
		}.launchIn(lifecycleScope)
	}

	private fun onStartRecording() {
		// configure notifications
		notificationHelper.setContentText(text = getString(R.string.recorder_notification_text_running))
		notificationHelper.setPauseStopAction()
		//start the recorder
		lifecycleScope.launch { voiceRecorder.startRecording() }
		// start foreground service
		startForegroundServiceMicrophone(
			NotificationConstants.RECORDER_NOTIFICATION_ID,
			notificationHelper.timerNotification
		)
	}

	private fun onStopRecording() {
		// stop the foreground
		stopForeground(Service.STOP_FOREGROUND_REMOVE)
		// stop the recording
		lifecycleScope.launch { voiceRecorder.stopRecording() }

	}

	private fun onResumeRecording() {
		//update the notification
		notificationHelper.setContentText(text = getString(R.string.recorder_notification_text_running))
		notificationHelper.setPauseStopAction()
		// notification notify
		_notificationManager?.notify(
			NotificationConstants.RECORDER_NOTIFICATION_ID,
			notificationHelper.timerNotification
		)
		//resume recording
		voiceRecorder.resumeRecording()
	}

	private fun onPauseRecording() {
		//update the notification
		notificationHelper.setContentText(text = getString(R.string.recorder_notification_text_paused))
		notificationHelper.setResumeStopAction()
		// notification update
		_notificationManager?.notify(
			NotificationConstants.RECORDER_NOTIFICATION_ID,
			notificationHelper.timerNotification
		)
		//pause recording
		voiceRecorder.pauseRecording()
	}

	private fun onCancelRecording() {
		// stop the foreground service
		stopForeground(STOP_FOREGROUND_REMOVE)
		// cancel recording
		lifecycleScope.launch { voiceRecorder.cancelRecording() }
	}


	override fun onDestroy() {
		// resources are cleared
		voiceRecorder.releaseResources()
		Log.d(LOGGER_TAG, "SERVICE DESTROYED")
		super.onDestroy()
	}
}

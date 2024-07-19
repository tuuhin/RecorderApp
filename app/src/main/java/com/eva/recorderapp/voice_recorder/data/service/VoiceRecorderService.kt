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
import com.eva.recorderapp.common.NOTIFICATION_TIMER_TIME_FORMAT
import com.eva.recorderapp.common.NotificationConstants
import com.eva.recorderapp.voice_recorder.data.util.startForegroundServiceMicrophone
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderStopWatch
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import javax.inject.Inject

private const val LOGGER_TAG = "VOICE RECORDER SERVICE"

@AndroidEntryPoint
class VoiceRecorderService : LifecycleService() {

	@Inject
	lateinit var voiceRecorder: VoiceRecorder

	@Inject
	lateinit var notificationHelper: NotificationHelper

	private val _notificationManager by lazy { getSystemService<NotificationManager>() }

	private val binder = LocalBinder()

	private val _amplitudes = MutableStateFlow(persistentListOf<Float>())

	/**
	 * A contineous flow of current sampled amplitudes by the [VoiceRecorder]
	 */
	val amplitides: StateFlow<ImmutableList<Float>>
		get() = _amplitudes.asStateFlow()

	/**
	 * Provides the [RecorderStopWatch] impl as [LocalTime] for the [VoiceRecorder]
	 */
	val recorderTime: StateFlow<LocalTime>
		get() = voiceRecorder.recorderTimer

	/**
	 * Provides the current [RecorderState] of the [VoiceRecorder]
	 */
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
		// creates the recorder
		voiceRecorder.createRecorder()
		// listen to changes
		try {
			// update the amplitude graph
			readAmplitudes()
			// read timer state
			readTimerAndUpdateNotification()
		} catch (e: CancellationException) {
			Log.d(LOGGER_TAG, "COROUTINE CANCELLED")
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
		}
		return super.onStartCommand(intent, flags, startId)
	}

	/**
	 * Reads the [VoiceRecorder.maxAmplitudes] and update the [VoiceRecorderService.amplitides]
	 * this methods seems working rather than directly reading [VoiceRecorder.maxAmplitudes]
	 */
	private fun readAmplitudes() {

		Log.d(LOGGER_TAG, "READING RECORDER AMPLITUDES")

		voiceRecorder.maxAmplitudes
			.catch { err -> Log.e(LOGGER_TAG, err.message ?: "ERROR", err) }
			.onEach { array ->

				val listbuilder = persistentListOf<Float>().builder()
				array.forEach(listbuilder::add)

				_amplitudes.update { listbuilder.build() }
			}.launchIn(lifecycleScope)
	}

	/**
	 * Read the [RecorderState] and update the notifications according to [VoiceRecorder]
	 */
	private fun readTimerAndUpdateNotification() {

		Log.d(LOGGER_TAG, "UPDATING NOTIFICATION STATE")

		combine(notificationTimer, recorderState) { time, state ->
			when (state) {
				RecorderState.RECORDING -> {

					val readableTime = time.format(NOTIFICATION_TIMER_TIME_FORMAT)
					//set the title
					notificationHelper.setContentTitle(readableTime)
					// show the notification
					_notificationManager?.notify(
						NotificationConstants.RECORDER_NOTIFICATION_ID,
						notificationHelper.timerNotification
					)
				}

				RecorderState.COMPLETED -> {
					notificationHelper.setContentText("Recording completed")
					notificationHelper.setContentTitle("Complete")
					_notificationManager?.notify(
						NotificationConstants.RECORDER_NOTIFICATION_ID,
						notificationHelper.recordingCompletedNotification
					)
				}

				else -> return@combine
			}
		}.launchIn(lifecycleScope)
	}

	private fun onStartRecording() {
		// configure notifications
		notificationHelper.setContentText(text = getString(R.string.recorder_notification_text_running))
		notificationHelper.setPauseStopAction()
		//start foreground service
		startForegroundServiceMicrophone(
			NotificationConstants.RECORDER_NOTIFICATION_ID,
			notificationHelper.timerNotification
		)
		//start thre recorder
		lifecycleScope.launch { voiceRecorder.startRecording() }
	}

	private fun onStopRecording() {
		// stop the recording
		lifecycleScope.launch { voiceRecorder.stopRecording() }
		stopForeground(Service.STOP_FOREGROUND_REMOVE)
	}

	private fun onResumeRecording() {
		//update the notification
		notificationHelper.setContentTitle("RUNNING RECORDER")
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

	override fun onDestroy() {
		// resources are cleared
		voiceRecorder.releaseResources()
		Log.d(LOGGER_TAG, "SERVICE DESTROYED")
		super.onDestroy()
	}
}

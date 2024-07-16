package com.eva.recorderapp.voice_recorder.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.ForegroundServiceTypeException
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.getSystemService
import com.eva.recorderapp.R
import com.eva.recorderapp.common.NOTIFICATION_TIMER_TIME_FORMAT
import com.eva.recorderapp.common.NotificationConstants
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import javax.inject.Inject

private const val LOGGER_TAG = "VOICE RECORDER SERVICE"

@AndroidEntryPoint
class VoiceRecorderService : Service() {

	@Inject
	lateinit var voiceRecorder: VoiceRecorder

	@Inject
	lateinit var notificationHelper: NotificationHelper

	private val _notificationManager by lazy { getSystemService<NotificationManager>() }

	private val scope = CoroutineScope(Dispatchers.IO)

	private val binder = LocalBinder()


	inner class LocalBinder : Binder() {
		fun getService(): VoiceRecorderService = this@VoiceRecorderService
	}

	override fun onBind(intent: Intent): IBinder? {
		Log.d(LOGGER_TAG, "SERVICE BINDED")
		return binder
	}

	override fun onCreate() {
		super.onCreate()
		// creates the recorder
		voiceRecorder.createRecorder()
		// listen to changes
		try {
			readTimeAndState()
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


	private fun readTimeAndState() {
		scope.launch {
			combine(
				voiceRecorder.recorderTimer,
				voiceRecorder.recorderState
			) { time, state ->
				when (state) {
					RecorderState.RECORDING -> {

						val readableTime = time.format(NOTIFICATION_TIMER_TIME_FORMAT)
						//set the title
						notificationHelper.setContentTitle(readableTime)
						// show the notification
						_notificationManager?.notify(
							NotificationConstants.RECORDER_NOTIFICATION_ID,
							notificationHelper.notification
						)
					}

					RecorderState.COMPLETED -> {
						Log.d(LOGGER_TAG, "COMPLETED")
					}

					else -> return@combine
				}
			}.launchIn(this)
		}
	}

	private fun startAppForegroundService() {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			try {
				startForeground(
					NotificationConstants.RECORDER_NOTIFICATION_ID,
					notificationHelper.notification,
					ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
				)
			} catch (e: ForegroundServiceTypeException) {
				Log.e(LOGGER_TAG, "WRONG FG-SERVICE TYPE", e)
			} catch (e: ForegroundServiceStartNotAllowedException) {
				Log.e(LOGGER_TAG, "FG-SERVICE NOT ALLOWED TO START", e)
			}
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			try {
				startForeground(
					NotificationConstants.RECORDER_NOTIFICATION_ID,
					notificationHelper.notification,
					ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
				)
			} catch (e: ForegroundServiceStartNotAllowedException) {
				Log.e(LOGGER_TAG, "FG-SERVICE NOT ALLOWED TO START", e)
			}
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			try {
				startForeground(
					NotificationConstants.RECORDER_NOTIFICATION_ID,
					notificationHelper.notification,
					ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
				)
			} catch (e: Exception) {
				Log.e(LOGGER_TAG, "SOME EXCEPTION OCCURED", e)
			}
		} else {
			try {
				startForeground(
					NotificationConstants.RECORDER_NOTIFICATION_ID,
					notificationHelper.notification
				)
			} catch (e: Exception) {
				Log.e(LOGGER_TAG, "SOME EXCEPTION OCCURED", e)
			}
		}
	}

	private fun onStartRecording() {
		// configure notifications
		notificationHelper.setContentTitle("RUNNING RECORDER")
		notificationHelper.setContentText(text = getString(R.string.recorder_notification_text_running))
		notificationHelper.setPauseStopAction()
		//start foreground service
		startAppForegroundService()
		//start thre recorder
		scope.launch { voiceRecorder.startRecording() }
	}

	private fun onStopRecording() {
		// stop the recording
		scope.launch { voiceRecorder.stopRecording() }
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
			notificationHelper.notification
		)
		//resume recording
		voiceRecorder.resumeRecording()
	}

	private fun onPauseRecording() {
		//update the notification
		notificationHelper.setContentText(text = getString(R.string.recorder_notification_text_paused))
		notificationHelper.setResumeStopAction()
		// notification notify
		_notificationManager?.notify(
			NotificationConstants.RECORDER_NOTIFICATION_ID,
			notificationHelper.notification
		)
		//pause recording
		voiceRecorder.pauseRecording()
	}

	override fun onDestroy() {
		scope.launch {
			voiceRecorder.releaseResources()
			// cancel the scope
			Log.d(LOGGER_TAG, "COROUTINE CANCELLED")
			cancel()
		}
		Log.d(LOGGER_TAG, "SERVICE DESTROYED")
		super.onDestroy()
	}
}
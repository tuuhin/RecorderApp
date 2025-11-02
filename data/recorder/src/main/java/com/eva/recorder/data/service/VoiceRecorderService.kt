package com.eva.recorder.data.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.eva.bookmarks.domain.provider.RecordingBookmarksProvider
import com.eva.recorder.domain.RecorderWidgetInteractor
import com.eva.recorder.domain.VoiceRecorder
import com.eva.recorder.domain.models.RecordedPoint
import com.eva.recorder.domain.models.RecorderAction
import com.eva.recorder.domain.models.RecorderState
import com.eva.use_case.usecases.BluetoothScoUseCase
import com.eva.use_case.usecases.PhoneStateObserverUseCase
import com.eva.utils.NotificationConstants
import com.eva.utils.Resource
import com.eva.utils.roundToClosestSeconds
import com.eva.utils.toDuration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val LOGGER_TAG = "VOICE_RECORDER_SERVICE"

@AndroidEntryPoint
internal class VoiceRecorderService : LifecycleService() {

	@Inject
	lateinit var voiceRecorder: VoiceRecorder

	@Inject
	lateinit var bluetoothScoUseCase: BluetoothScoUseCase

	@Inject
	lateinit var phoneStateObserverUseCase: PhoneStateObserverUseCase

	@Inject
	lateinit var bookmarksProvider: RecordingBookmarksProvider

	@Inject
	lateinit var widgetFacade: RecorderWidgetInteractor

	@Inject
	lateinit var notificationHelper: NotificationHelper

	private val binder = LocalBinder()

	private val _bookMarks = MutableStateFlow(emptySet<LocalTime>())

	@OptIn(ExperimentalCoroutinesApi::class)
	val bookMarks = _bookMarks.mapLatest { bookMarks ->
		// convert it to set such that common items are subtracted
		bookMarks.map(LocalTime::roundToClosestSeconds)
			.map(LocalTime::toDuration)
			.toSet()
	}

	val amplitudes: Flow<List<RecordedPoint>>
		get() = voiceRecorder.dataPoints

	val recorderState: StateFlow<RecorderState>
		get() = voiceRecorder.recorderState

	@OptIn(FlowPreview::class)
	private val notificationTimer: Flow<LocalTime>
		get() = voiceRecorder.recorderTimer
			.distinctUntilChanged { old, new -> old.second == new.second }

	@OptIn(FlowPreview::class)
	val recorderTime: Flow<LocalTime>
		get() = voiceRecorder.recorderTimer.sample(100.milliseconds)

	/**
	 * Recorder timer sampled per 1 seconds
	 */
	@OptIn(FlowPreview::class)
	private val widgetTimer: Flow<LocalTime>
		get() = voiceRecorder.recorderTimer.sample(1.seconds)

	inner class LocalBinder : Binder() {

		fun getService(): VoiceRecorderService = this@VoiceRecorderService
	}

	override fun onBind(intent: Intent): IBinder {
		super.onBind(intent)
		Log.d(LOGGER_TAG, "SERVICE BOUNDED")
		return binder
	}

	override fun onCreate() {
		super.onCreate()
		try {
			// check use case
			lifecycleScope.launch {
				bluetoothScoUseCase.startConnectionIfAllowed()
			}
			// read phone states
			observeChangingPhoneState()
			// inform bt connect
			showBluetoothConnectedToast()
			// update the notification
			readTimerAndUpdateNotification()
			// update widget state
			updateRecorderWidgetState()
			Log.i(LOGGER_TAG, "SERVICE CREATED WITH OBSERVERS")
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		when (intent?.action) {
			RecorderAction.StartRecorderAction.action -> onStartRecording()
			RecorderAction.ResumeRecorderAction.action -> onResumeRecording()
			RecorderAction.PauseRecorderAction.action -> onPauseRecording()
			RecorderAction.StopRecorderAction.action -> onStopRecording()
			RecorderAction.CancelRecorderAction.action -> onCancelRecording()
			RecorderAction.AddBookMarkAction.action -> addBookMark()
		}
		return super.onStartCommand(intent, flags, startId)
	}


	private fun showBluetoothConnectedToast() = lifecycleScope.launch {
		bluetoothScoUseCase.observeConnectedState(onStateConnected = ::showScoConnectToast)
	}

	private fun observeChangingPhoneState() {
		recorderState.onEach {
			phoneStateObserverUseCase.checkIfAllowedAndRinging(
				isRecording = it == RecorderState.RECORDING,
				onPhoneRinging = {
					phoneRingingToast()
					onPauseRecording()
				},
			)

		}.launchIn(lifecycleScope)
	}


	private fun readTimerAndUpdateNotification() {
		combine(notificationTimer, recorderState) { time, state ->
			when (state) {
				RecorderState.RECORDING -> notificationHelper.showNotificationDuringRecording(time)
				RecorderState.COMPLETED -> notificationHelper.showRecordingDoneNotification()
				else -> {}
			}
		}.launchIn(lifecycleScope)
	}

	private fun updateRecorderWidgetState() {
		combine(widgetTimer, recorderState) { time, state ->
			// update the widget
			widgetFacade.updateWidget(state, time)
		}.launchIn(lifecycleScope)
	}

	private fun onStartRecording() {
		//start the recorder
		lifecycleScope.launch { voiceRecorder.startRecording() }.invokeOnCompletion {
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
		lifecycleScope.launch {
			voiceRecorder.resumeRecording()
		}
	}

	private fun onPauseRecording() {
		//update the notification
		notificationHelper.setOnPauseNotification()
		//pause recording
		lifecycleScope.launch {
			voiceRecorder.pauseRecording()
		}
	}

	private fun onCancelRecording() {
		lifecycleScope.launch {
			// cancel recording
			voiceRecorder.cancelRecording()
			//clear bookmarks
			clearBookMarks()
			//update widget
			widgetFacade.resetWidget()
		}.invokeOnCompletion {
			// stop the foreground
			stopForeground(STOP_FOREGROUND_REMOVE)
			// stop the service
			stopSelf()
		}
	}

	private fun onStopRecording() {
		// stop the recording
		lifecycleScope.launch {
			val timeBeforeSave = voiceRecorder.recorderTimer.value
			voiceRecorder.stopRecording().fold(
				onSuccess = { recordingId ->
					clearAndSaveBookMarks(recordingId, timeBeforeSave)
					// again show the notification
					notificationHelper.showCompletedNotificationWithIntent(recordingId)
				},
				onFailure = { error ->
					val message = error.message ?: ""
					showSaveRecordingErrorMessage(message)
				},
			)
			//update widget
			widgetFacade.resetWidget()
		}.invokeOnCompletion {
			//clear and save bookmarks
			// stop the foreground
			stopForeground(STOP_FOREGROUND_REMOVE)
			// stop the service
			stopSelf()
		}
	}

	private fun addBookMark() {
		val timeWhenClicked = voiceRecorder.recorderTimer.value
		//should be a multiple of 100
		val closestSecond = timeWhenClicked.roundToClosestSeconds()
		if (closestSecond <= timeWhenClicked) {
			// add it to bookmarks
			Log.d(LOGGER_TAG, "BOOKMARKS ADDED :$closestSecond")
			_bookMarks.update { it + closestSecond }
		}
	}

	private suspend fun clearAndSaveBookMarks(
		recordingId: Long,
		lastRecordedTime: LocalTime,
	) {
		// no need to perform any actions if bookmarks is empty
		if (_bookMarks.value.isEmpty()) return
		// add the coroutines on a different coroutine
		val job = lifecycleScope.launch {
			// filter the bookmarks
			val bookmarks = _bookMarks.value
				.filter { it <= lastRecordedTime }
				.map { it.roundToClosestSeconds() }
				.toSet()

			Log.d(LOGGER_TAG, "SAVING ${bookmarks.size} BOOKMARKS ")

			val result = bookmarksProvider.createBookMarks(
				recordingId = recordingId,
				points = bookmarks
			)
			when (result) {
				is Resource.Error -> {
					val message = result.message ?: result.error.message ?: "ERROR"
					Log.wtf(LOGGER_TAG, message)
				}

				is Resource.Success -> showBookmarksSavedMessage()
				else -> {}
			}
		}
		Log.d(LOGGER_TAG, "BOOKMARKS SAVED")
		// add logic to save bookmarks
		Log.d(LOGGER_TAG, "BOOKMARKS CLEARED")
		_bookMarks.update { emptySet() }
		// waits for the job completion
		job.join()
	}

	private fun clearBookMarks() {
		Log.d(LOGGER_TAG, "BOOKMARKS CLEARED")
		_bookMarks.update { emptySet() }
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
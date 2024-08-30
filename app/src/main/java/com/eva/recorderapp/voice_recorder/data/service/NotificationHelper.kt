package com.eva.recorderapp.voice_recorder.data.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.eva.recorderapp.MainActivity
import com.eva.recorderapp.R
import com.eva.recorderapp.common.IntentRequestCodes
import com.eva.recorderapp.common.LocalTimeFormats
import com.eva.recorderapp.common.NotificationConstants
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format

class NotificationHelper(
	private val context: Context
) {

	private val _notificationManager by lazy { context.getSystemService<NotificationManager>() }

	private val recorderServiceIntent: Intent
		get() = Intent(context, VoiceRecorderService::class.java)

	private val recorderScreenIntent: Intent
		get() = Intent(context, MainActivity::class.java).apply {
			data = NavDeepLinks.recorderDestinationUri
			action = Intent.ACTION_VIEW
		}

	private val recordingsScreenIntent: Intent
		get() = Intent(context, MainActivity::class.java).apply {
			data = NavDeepLinks.recordingsDestinationUri
			action = Intent.ACTION_VIEW
		}

	private fun buildServicePendingIntent(
		context: Context,
		requestCodes: IntentRequestCodes,
		intent: Intent,
		flags: Int = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
	) = PendingIntent.getService(context, requestCodes.code, intent, flags)

	private fun buildActivityPendingIntent(
		context: Context,
		requestCodes: IntentRequestCodes,
		intent: Intent,
		flags: Int = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
	) = PendingIntent.getActivity(context, requestCodes.code, intent, flags)


	private val recorderScreenPendingIntent: PendingIntent
		get() = buildActivityPendingIntent(
			context = context,
			requestCodes = IntentRequestCodes.ACTIVITY_INTENT_RECORDER,
			intent = recorderScreenIntent,
		)

	private val recordingsScreenPendingIntent: PendingIntent
		get() = buildActivityPendingIntent(
			context,
			IntentRequestCodes.ACTIVITY_INTENT_RECORDINGS,
			intent = recordingsScreenIntent,
		)

	private val stopRecorderPendingIntent: PendingIntent
		get() = buildServicePendingIntent(
			context = context,
			requestCodes = IntentRequestCodes.STOP_VOICE_RECORDER,
			intent = recorderServiceIntent.apply {
				action = RecorderAction.STOP_RECORDER.action
			}
		)

	private val cancelRecorderPendingIntent: PendingIntent
		get() = buildServicePendingIntent(
			context = context,
			requestCodes = IntentRequestCodes.CANCEL_VOICE_RECORDER,
			intent = recorderServiceIntent.apply {
				action = RecorderAction.CANCEL_RECORDER.action
			}
		)

	private val pauseRecorderPendingIntent: PendingIntent
		get() = buildServicePendingIntent(
			context = context,
			requestCodes = IntentRequestCodes.PAUSE_VOICE_RECORDER,
			intent = recorderServiceIntent.apply {
				action = RecorderAction.PAUSE_RECORDER.action
			}
		)

	private val resumeRecorderPendingIntent: PendingIntent
		get() = buildServicePendingIntent(
			context = context,
			requestCodes = IntentRequestCodes.RESUME_VOICE_RECORDER,
			intent = recorderServiceIntent.apply {
				action = RecorderAction.RESUME_RECORDER.action
			}
		)

	private val recorderCustomView = RemoteViews(
		context.packageName, R.layout.recorder_remote_view_layout
	).apply {
		setOnClickPendingIntent(R.id.stop_button, stopRecorderPendingIntent)
		setOnClickPendingIntent(R.id.cancel_button, cancelRecorderPendingIntent)
		setOnClickPendingIntent(R.id.resume_button, resumeRecorderPendingIntent)
		setOnClickPendingIntent(R.id.pause_button, pauseRecorderPendingIntent)
	}

	private val _recorderNotification =
		NotificationCompat.Builder(context, NotificationConstants.RECORDER_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_microphone)
			.setStyle(NotificationCompat.DecoratedCustomViewStyle())
			.setCustomContentView(recorderCustomView)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setSilent(true)
			.setShowWhen(false)
			.setOnlyAlertOnce(true)
			.setOngoing(true)
			.setContentIntent(recorderScreenPendingIntent)

	private val recordingCompleteNotification: Notification =
		NotificationCompat.Builder(context, NotificationConstants.RECORDER_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_outlined_recording)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setContentTitle(context.getString(R.string.recorder_recording_completed))
			.setContentText(context.getString(R.string.recorder_recording_completed_text))
			.setAutoCancel(true)
			.setContentIntent(recordingsScreenPendingIntent)
			.build()

	private val recordingCancelNotification: Notification =
		NotificationCompat.Builder(context, NotificationConstants.RECORDER_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_outlined_recording)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setContentTitle(context.getString(R.string.recorder_recording_notificaiton_title_canceled))
			.setContentText(context.getString(R.string.recorder_recording_notification_canceled_text))
			.setAutoCancel(true)
			.build()

	val timerNotification: Notification
		get() {
			val updatedRemoteView = recorderCustomView.apply {
				setTextViewText(
					R.id.notification_text,
					context.getString(R.string.recorder_notification_text_running)
				)
				setViewVisibility(R.id.pause_button, View.VISIBLE)
				setViewVisibility(R.id.resume_button, View.GONE)
			}

			return _recorderNotification
				.setCustomContentView(updatedRemoteView)
				.build()
		}


	fun showNotificationDuringRecording(time: LocalTime) {
		val readableTime = time.format(LocalTimeFormats.NOTIFICATION_TIMER_TIME_FORMAT)
		val updatedRemoteView = recorderCustomView.apply {
			setTextViewText(R.id.notification_title, readableTime)
		}
		// show the notification
		_notificationManager?.notify(
			NotificationConstants.RECORDER_NOTIFICATION_ID,
			_recorderNotification
				.setCustomContentView(updatedRemoteView)
				.build()
		)
	}

	fun setRecordingsCompletedNotification() {
		_notificationManager?.notify(
			NotificationConstants.RECORDER_NOTIFICATION_SECONDARY_ID,
			recordingCompleteNotification
		)
	}

	fun setRecordingCancelNotification() {
		_notificationManager?.notify(
			NotificationConstants.RECORDER_NOTIFICATION_SECONDARY_ID,
			recordingCancelNotification
		)
	}

	fun setOnPauseNotification() {
		val updatedRemoteView = recorderCustomView.apply {
			setTextViewText(
				R.id.notification_text,
				context.getString(R.string.recorder_notification_text_paused)
			)
			setViewVisibility(R.id.pause_button, View.GONE)
			setViewVisibility(R.id.resume_button, View.VISIBLE)
		}
		// show the notification
		_notificationManager?.notify(
			NotificationConstants.RECORDER_NOTIFICATION_ID,
			_recorderNotification
				.setCustomContentView(updatedRemoteView)
				.build()
		)
	}

	fun setOnResumeNotification() {
		val updatedRemoteView = recorderCustomView.apply {
			setTextViewText(
				R.id.notification_text,
				context.getString(R.string.recorder_notification_text_running)
			)
			setViewVisibility(R.id.pause_button, View.VISIBLE)
			setViewVisibility(R.id.resume_button, View.GONE)
		}
		// notification notify
		_notificationManager?.notify(
			NotificationConstants.RECORDER_NOTIFICATION_ID,
			_recorderNotification
				.setCustomContentView(updatedRemoteView)
				.build()
		)
	}

}
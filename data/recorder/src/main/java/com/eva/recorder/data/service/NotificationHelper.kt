package com.eva.recorder.data.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.eva.recorder.R
import com.eva.recorder.domain.models.RecorderAction
import com.eva.utils.IntentConstants
import com.eva.utils.IntentRequestCodes
import com.eva.utils.LocalTimeFormats
import com.eva.utils.NavDeepLinks
import com.eva.utils.NotificationConstants
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format

internal class NotificationHelper(private val context: Context) {

	private val _notificationManager by lazy { context.getSystemService<NotificationManager>() }

	private val recorderServiceIntent: Intent
		get() = Intent(context, VoiceRecorderService::class.java)

	private val recorderScreenIntent: Intent
		get() = Intent().apply {
			setClassName(context.applicationContext, IntentConstants.MAIN_ACTIVITY)
			data = NavDeepLinks.RECORDER_DESTINATION_PATTERN.toUri()
			action = Intent.ACTION_VIEW
		}

	private val recordingsScreenIntent: Intent
		get() = Intent().apply {
			setClassName(context.applicationContext, IntentConstants.MAIN_ACTIVITY)
			data = NavDeepLinks.RECORDING_DESTINATION_PATTERN.toUri()
			action = Intent.ACTION_VIEW
		}

	private fun buildServicePendingIntent(
		context: Context,
		requestCodes: IntentRequestCodes,
		intent: Intent,
		flags: Int = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT,
	) = PendingIntent.getService(context, requestCodes.code, intent, flags)

	private fun buildActivityPendingIntent(
		context: Context,
		requestCodes: IntentRequestCodes,
		intent: Intent,
		flags: Int = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT,
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
				action = RecorderAction.StopRecorderAction.action
			},
		)

	private val cancelRecorderPendingIntent: PendingIntent
		get() = buildServicePendingIntent(
			context = context,
			requestCodes = IntentRequestCodes.CANCEL_VOICE_RECORDER,
			intent = recorderServiceIntent.apply {
				action = RecorderAction.CancelRecorderAction.action
			},
		)

	private val pauseRecorderPendingIntent: PendingIntent
		get() = buildServicePendingIntent(
			context = context,
			requestCodes = IntentRequestCodes.PAUSE_VOICE_RECORDER,
			intent = recorderServiceIntent.apply {
				action = RecorderAction.PauseRecorderAction.action
			},
		)

	private val resumeRecorderPendingIntent: PendingIntent
		get() = buildServicePendingIntent(
			context = context,
			requestCodes = IntentRequestCodes.RESUME_VOICE_RECORDER,
			intent = recorderServiceIntent.apply {
				action = RecorderAction.ResumeRecorderAction.action
			},
		)

	private fun buildPendingIntentToPlayer(recordingId: Long): PendingIntent {
		val intent = Intent().apply {
			setClassName(context.applicationContext, IntentConstants.MAIN_ACTIVITY)
			data = NavDeepLinks.audioPlayerDestinationUri(recordingId).toUri()
			action = Intent.ACTION_VIEW
		}

		return buildActivityPendingIntent(
			context,
			IntentRequestCodes.PLAYER_NOTIFICATION_INTENT,
			intent
		)
	}

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
			.setPriority(NotificationCompat.PRIORITY_MAX)
			.setCategory(NotificationCompat.CATEGORY_PROGRESS)
			.setShowWhen(false)
			.setOnlyAlertOnce(true)
			.setOngoing(true)
			.setContentIntent(recorderScreenPendingIntent)

	private val _recordingCompleteNotification =
		NotificationCompat.Builder(context, NotificationConstants.RECORDING_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_outlined_recording)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setContentTitle(context.getString(R.string.recorder_recording_completed))
			.setContentText(context.getString(R.string.recorder_recording_completed_text))
			.setAutoCancel(true)
			.setContentIntent(recordingsScreenPendingIntent)


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

	fun showRecordingDoneNotification() {
		_notificationManager?.notify(
			NotificationConstants.RECORDER_NOTIFICATION_SECONDARY_ID,
			_recordingCompleteNotification.build()
		)
	}

	fun showCompletedNotificationWithIntent(recordingId: Long) {
		_notificationManager?.notify(
			NotificationConstants.RECORDER_NOTIFICATION_SECONDARY_ID,
			_recordingCompleteNotification
				.setContentText(context.getString(R.string.recorder_recording_completed_text_2))
				.setContentIntent(buildPendingIntentToPlayer(recordingId))
				.build()
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
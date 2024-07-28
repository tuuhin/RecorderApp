package com.eva.recorderapp.voice_recorder.data.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.eva.recorderapp.MainActivity
import com.eva.recorderapp.R
import com.eva.recorderapp.common.IntentRequestCodes
import com.eva.recorderapp.common.NotificationConstants
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks

class NotificationHelper(
	private val context: Context
) {

	private val manager by lazy { context.getSystemService<NotificationManager>() }

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

	private fun buildNotificationAction(
		title: String,
		intent: PendingIntent? = null
	) = NotificationCompat.Action.Builder(0, title, intent).build()

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


	private val pauseRecorderAction: NotificationCompat.Action
		get() = buildNotificationAction(
			title = context.getString(R.string.recorder_action_pause),
			intent = buildServicePendingIntent(
				context = context,
				requestCodes = IntentRequestCodes.PAUSE_VOICE_RECORDER,
				intent = recorderServiceIntent.apply {
					action = RecorderAction.PAUSE_RECORDER.action
				}
			)
		)

	private val resumeRecorderAction: NotificationCompat.Action
		get() = buildNotificationAction(
			title = context.getString(R.string.recorder_action_resume),
			intent = buildServicePendingIntent(
				context = context,
				requestCodes = IntentRequestCodes.RESUME_VOICE_RECORDER,
				intent = recorderServiceIntent.apply {
					action = RecorderAction.RESUME_RECORDER.action
				}
			)
		)


	private val stopRecorderAction: NotificationCompat.Action
		get() = buildNotificationAction(
			title = context.getString(R.string.recorder_action_stop),
			intent = buildServicePendingIntent(
				context = context,
				requestCodes = IntentRequestCodes.STOP_VOICE_RECORDER,
				intent = recorderServiceIntent.apply {
					action = RecorderAction.STOP_RECORDER.action
				}
			)
		)

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

	private val _recorderNotifcation =
		NotificationCompat.Builder(context, NotificationConstants.RECORDER_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setSilent(true)
			.setOnlyAlertOnce(true)
			.setOngoing(true)
			.setContentIntent(recorderScreenPendingIntent)

	val recordingCompleteNotification: Notification =
		NotificationCompat.Builder(context, NotificationConstants.RECORDER_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setContentTitle(context.getString(R.string.recorder_recording_completed))
			.setContentText(context.getString(R.string.recorder_recording_completed_text))
			.setAutoCancel(true)
			.setOnlyAlertOnce(true)
			.setContentIntent(recordingsScreenPendingIntent)
			.build()

	val recordingCancelNotificaiton: Notification =
		NotificationCompat.Builder(context, NotificationConstants.RECORDER_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_launcher_foreground)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setContentTitle(context.getString(R.string.recorder_recording_notificaiton_title_canceled))
			.setContentText(context.getString(R.string.recorder_recording_notification_canceled_text))
			.setAutoCancel(true)
			.setOnlyAlertOnce(true)
			.build()

	val timerNotification: Notification
		get() = _recorderNotifcation.build()

	@SuppressLint("RestrictedApi")
	fun setPauseStopAction() {
		_recorderNotifcation.apply {
			val actions = arrayListOf(pauseRecorderAction, stopRecorderAction)
			mActions = actions
		}
	}

	@SuppressLint("RestrictedApi")
	fun setResumeStopAction() {
		_recorderNotifcation.apply {
			val actions = arrayListOf(resumeRecorderAction, stopRecorderAction)
			mActions = actions
		}
	}

	fun setContentTitle(title: String) {
		_recorderNotifcation.setContentTitle(title)
	}

	fun setContentText(text: String) {
		_recorderNotifcation.setContentText(text)
	}

}
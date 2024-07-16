package com.eva.recorderapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.eva.recorderapp.common.NotificationConstants
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RecorderApp : Application() {

	private val notificationManager by lazy { getSystemService<NotificationManager>() }

	override fun onCreate() {
		super.onCreate()
		val channelId = NotificationConstants.RECORDER_CHANNEL_ID
		val channelName = NotificationConstants.RECORDER_CHANNEL_NAME
		val channelDescription = NotificationConstants.RECORDER_CHANNEL_DESC

		val channel = NotificationChannel(
			channelId,
			channelName,
			NotificationManager.IMPORTANCE_HIGH
		).apply {
			description = channelDescription
			setShowBadge(false)
			lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
		}
		notificationManager?.createNotificationChannel(channel)
	}
}
package com.eva.recorderapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.eva.recorderapp.common.NotificationConstants
import com.eva.recorderapp.voice_recorder.data.worker.RemoveTrashRecordingWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RecorderApp : Application(), Configuration.Provider {

	private val notificationManager by lazy { getSystemService<NotificationManager>() }

	@Inject
	lateinit var workerFatory: HiltWorkerFactory

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFatory)
			.build()


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

		//start wokers
		RemoveTrashRecordingWorker.startRepeatWorker(applicationContext)
	}
}
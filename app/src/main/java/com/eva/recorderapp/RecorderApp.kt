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
import com.eva.recorderapp.voice_recorder.presentation.util.AppShortCuts
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

		val channel1 = NotificationChannel(
			NotificationConstants.RECORDER_CHANNEL_ID,
			NotificationConstants.RECORDER_CHANNEL_NAME,
			NotificationManager.IMPORTANCE_HIGH
		).apply {
			description = NotificationConstants.RECORDER_CHANNEL_DESC
			setShowBadge(false)
			lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
		}

		val channel2 = NotificationChannel(
			NotificationConstants.PLAYER_CHANNEL_ID,
			NotificationConstants.PLAYER_CHANNEL_NAME,
			NotificationManager.IMPORTANCE_DEFAULT
		).apply {
			description = NotificationConstants.PLAYER_CHANNEL_DESC
			setShowBadge(false)
			lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
		}

		val channels = listOf(channel1, channel2)

		notificationManager?.createNotificationChannels(channels)

		//start wokers
		RemoveTrashRecordingWorker.startRepeatWorker(applicationContext)

		//shortcuts
		AppShortCuts(this).attachShortcutsIfNotPresent()
	}
}
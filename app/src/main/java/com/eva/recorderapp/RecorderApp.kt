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
import com.eva.recorderapp.voice_recorder.data.worker.UpdateRecordingPathWorker
import com.eva.recorderapp.voice_recorder.domain.util.AppShortcutFacade
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RecorderApp : Application(), Configuration.Provider {

	private val notificationManager by lazy { getSystemService<NotificationManager>() }

	@Inject
	lateinit var workerFactory: HiltWorkerFactory

	@Inject
	lateinit var shortcutFacade: AppShortcutFacade

	override val workManagerConfiguration: Configuration
		get() = Configuration.Builder()
			.setWorkerFactory(workerFactory)
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
			NotificationConstants.RECORDING_CHANNEL_ID,
			NotificationConstants.RECORDING_CHANNEL_NAME,
			NotificationManager.IMPORTANCE_DEFAULT
		).apply {
			description = NotificationConstants.RECORDING_CHANNEL_DESC
			setShowBadge(false)
			lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
		}

		val channel3 = NotificationChannel(
			NotificationConstants.PLAYER_CHANNEL_ID,
			NotificationConstants.PLAYER_CHANNEL_NAME,
			NotificationManager.IMPORTANCE_MIN
		).apply {
			description = NotificationConstants.PLAYER_CHANNEL_DESC
			lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
		}

		val channels = listOf(channel1, channel2, channel3)

		notificationManager?.createNotificationChannels(channels)

		//shortcuts
		shortcutFacade.createRecordingsShortCut()

		//start workers
		RemoveTrashRecordingWorker.startRepeatWorker(applicationContext)
		// update path worker
		UpdateRecordingPathWorker.startWorker(applicationContext)
	}
}
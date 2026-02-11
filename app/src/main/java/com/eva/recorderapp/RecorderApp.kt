package com.eva.recorderapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.StrictMode
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.tooling.ComposeStackTraceMode
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.eva.interactions.domain.AppShortcutFacade
import com.eva.utils.NotificationConstants
import com.eva.worker.RemoveTrashRecordingWorker
import com.eva.worker.UpdateRecordingPathWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@OptIn(ExperimentalComposeRuntimeApi::class)
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

		enableComposeStackTrace()
		enableStrictMode()

		createNotificationChannels()

		//shortcuts
		shortcutFacade.createRecordingsShortCut()

		//start workers
		RemoveTrashRecordingWorker.startRepeatWorker(applicationContext)
		// update path worker
		UpdateRecordingPathWorker.startWorker(applicationContext)
	}


	private fun createNotificationChannels() {
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

		val channel4 = NotificationChannel(
			NotificationConstants.WORKER_CHANNEL_ID,
			NotificationConstants.WORKER_CHANNEL_NAME,
			NotificationManager.IMPORTANCE_HIGH
		).apply {
			description = NotificationConstants.WORKER_CHANNEL_DESC
			lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
		}

		val channels = listOf(channel1, channel2, channel3, channel4)

		notificationManager?.createNotificationChannels(channels)
	}


	private fun enableComposeStackTrace() {
		if (!BuildConfig.DEBUG) return
		Composer.setDiagnosticStackTraceMode(ComposeStackTraceMode.SourceInformation)
	}

	private fun enableStrictMode() {
		if (!BuildConfig.DEBUG) return
		// thread policy
		StrictMode.setThreadPolicy(
			StrictMode.ThreadPolicy.Builder()
				.detectAll()  // Detect all thread violations
				.penaltyLog() // Log to console
				.build()
		)

		StrictMode.setVmPolicy(
			StrictMode.VmPolicy.Builder()
				.detectAll()  // Detect all VM violations
				.penaltyLog()
				.build()
		)
	}
}
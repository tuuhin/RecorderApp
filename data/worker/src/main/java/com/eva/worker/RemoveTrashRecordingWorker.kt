package com.eva.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.eva.recordings.domain.tasks.RemoveTrashRecordingTask
import com.eva.utils.NotificationConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration

@HiltWorker
class RemoveTrashRecordingWorker @AssistedInject constructor(
	@Assisted private val context: Context,
	@Assisted private val params: WorkerParameters,
	private val task: RemoveTrashRecordingTask,
) : CoroutineWorker(context, params) {

	override suspend fun doWork(): Result {
		// no need to do anything for Api 30 as removing items is automatically handled
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return Result.success()
		// otherwise
		val notification = createNotification()
		// foreground info to indicate something is going on
		setForegroundAsync(
			ForegroundInfo(
				NotificationConstants.DELETE_WORKER_NOTIFICATION_ID,
				notification,
				ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
			)
		)
		val result = task.invoke()
		return if (result.isSuccess)
			Result.success(
				workDataOf(WorkerParams.UPDATE_RECORDING_PATH_SUCCESS_KEY to "EXECUTED SUCCESSFULLY")
			)
		else {
			val err = result.exceptionOrNull()
			val message = err?.message ?: "SOME ERROR OCCURRED"
			Result.failure(
				workDataOf(WorkerParams.UPDATE_RECORDING_PATH_FAILED_KEY to message)
			)
		}
	}

	private fun createNotification(): Notification {
		return NotificationCompat
			.Builder(context, NotificationConstants.RECORDER_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_broom)
			.setContentTitle("Clearing Trash")
			.setContentText("Removing expired recordings")
			.setAutoCancel(true)
			.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.build()
	}

	companion object {

		private val constrains = Constraints.Builder()
//			.setRequiresDeviceIdle(true)
//			.setRequiresBatteryNotLow(true)
			.build()

		private const val UNIQUE_NAME = "REMOVE_TRASH_WORKER"

		private val workRequest =
			PeriodicWorkRequestBuilder<RemoveTrashRecordingWorker>(
				repeatInterval = Duration.ofDays(1)
			)
				.setBackoffCriteria(BackoffPolicy.LINEAR, Duration.ofMinutes(30))
				.setInitialDelay(Duration.ofSeconds(5))
				.setConstraints(constrains)
				.build()

		fun startRepeatWorker(context: Context) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return
			// only add this for android 10

			val workManager = WorkManager.getInstance(context)

			workManager.enqueueUniquePeriodicWork(
				UNIQUE_NAME,
				ExistingPeriodicWorkPolicy.KEEP,
				workRequest
			)

		}

		fun cancelWork(context: Context) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) return

			//cancel worker only for android 10
			val workManager = WorkManager.getInstance(context)
			workManager.cancelUniqueWork(UNIQUE_NAME)
		}

	}
}
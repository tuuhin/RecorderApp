package com.eva.recorderapp.voice_recorder.data.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toFile
import androidx.core.net.toUri
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
import com.eva.recorderapp.R
import com.eva.recorderapp.common.NotificationConstants
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.files.TrashRecordingsProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.time.Duration
import kotlin.time.DurationUnit

private const val TAG = "TRASH_WORKER"

@HiltWorker
class RemoveTrashRecordingWorker @AssistedInject constructor(
	@Assisted private val context: Context,
	@Assisted private val params: WorkerParameters,
	private val trashRecorder: TrashRecordingsProvider,
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

		return withContext(Dispatchers.IO) {
			val resource = trashRecorder.getTrashedVoiceRecordings()
			when (resource) {
				is Resource.Success -> {
					val currentInstant = Clock.System.now()
					// delete-now delete is alawys at future so
					// delete-now is alawys postive except the case of
					// expires then delete-now is negative
					val expiredByTime = resource.data.filter { model ->
						val deleteInstant =
							model.expiresAt.toInstant(TimeZone.currentSystemDefault())
						val diff = deleteInstant.minus(currentInstant)
						diff.toLong(DurationUnit.MINUTES) < 0
					}
					// if mistakenly some files are deleted but the metadata already exits then
					// delete then
					val expiredByDelete = resource.data.filter { model ->
						val fileUri = model.fileUri.toUri()
						if (fileUri.scheme == "file") return@filter false
						val file = fileUri.toFile()
						// take if file and file dont exits or file is empty
						file.isFile && (!file.exists() || file.length() != 0L)
					}

					Log.d(TAG, "DELETING THE FILES NOW...")
					val trashModels = expiredByDelete union expiredByTime
					val deleteRecording = trashRecorder
						.permanentlyDeleteRecordedVoicesInTrash(trashModels)
					Log.d(TAG, "NO. OF FILES DELETED :${trashModels.size} ")

					(deleteRecording as? Resource.Error)?.let { error ->
						val errorMessage = "DELETE FAILED :${error.message ?: "UNKNONWN"}"
						Log.d(TAG, "ERROR OCCCURED :$errorMessage ")
						return@withContext Result.failure(
							workDataOf(
								WorkerParams.REMOVE_TRASH_RECORDING_FAILED_KEY to errorMessage
							)
						)
					}

					val successMessage = "FILES TO BE DELETED TODAY ${trashModels.size} "
					Result.success(
						workDataOf(
							WorkerParams.REMOVE_TRASH_RECORDING_SUCCESS_KEY to successMessage
						)
					)
				}

				is Resource.Error -> {
					val errorMessage = "DELETE FAILED :${resource.error.message ?: "UNKNONWN"}"
					Log.d(TAG, "ERROR OCCCURED :$errorMessage ")
					Result.failure(
						workDataOf(
							WorkerParams.REMOVE_TRASH_RECORDING_FAILED_KEY to errorMessage
						)
					)
				}
				// its an invalid state so no need to check or attach some message
				Resource.Loading -> Result.failure()
			}
		}
	}

	private fun createNotification(): Notification {
		return NotificationCompat
			.Builder(context, NotificationConstants.RECORDER_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_launcher_foreground)
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
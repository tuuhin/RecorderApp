package com.eva.worker

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.eva.recordings.domain.tasks.RecordingPathCorrectionTask
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

@HiltWorker
class UpdateRecordingPathWorker @AssistedInject constructor(
	@Assisted private val context: Context,
	@Assisted private val params: WorkerParameters,
	private val pathCorrector: RecordingPathCorrectionTask,
) : CoroutineWorker(context, params) {

	override suspend fun doWork(): Result {
		// no need to update the path if it's a fresh install or api31 below
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return Result.success()
		// recording path is available from API31+
		return withContext(Dispatchers.IO) {
			val result = pathCorrector.invoke()
			if (result.isSuccess)
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
	}

	companion object {

		private const val UNIQUE_NAME = "UPDATE_RECORDING_PATH_WORKER"

		private val workRequest = OneTimeWorkRequestBuilder<UpdateRecordingPathWorker>()
			.setInitialDelay(Duration.ofSeconds(5))
			.build()

		fun startWorker(context: Context) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

			val workManager = WorkManager.getInstance(context)

			workManager.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, workRequest)
		}

		fun cancelWork(context: Context) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

			//cancel worker only for android 10
			val workManager = WorkManager.getInstance(context)
			workManager.cancelUniqueWork(UNIQUE_NAME)
		}

	}
}
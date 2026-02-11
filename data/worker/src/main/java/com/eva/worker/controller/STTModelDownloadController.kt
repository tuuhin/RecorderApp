package com.eva.worker.controller

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.eva.worker.STTModelDownloaderWorker
import com.eva.worker.WorkerParams
import java.time.Duration
import java.util.UUID

class STTModelDownloadController(private val context: Context) {

	private val constrains by lazy {
		Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.setRequiresStorageNotLow(true)
			.setRequiresBatteryNotLow(true)
			.build()
	}


	fun startWorker(modelId: String): UUID {

		val workManager = WorkManager.getInstance(context)

		val workRequest = OneTimeWorkRequestBuilder<STTModelDownloaderWorker>()
			.setBackoffCriteria(BackoffPolicy.LINEAR, Duration.ofMinutes(30))
			.setInitialDelay(Duration.ofSeconds(2))
			.addTag("DOWNLOAD_STT_MODEL_WORKER")
			.setInputData(workDataOf(WorkerParams.WORK_DATA_STT_MODEL_ID to modelId))
			.setConstraints(constrains)
			.build()

		workManager.enqueue(workRequest)

		return workRequest.id
	}

	fun cancelWork(requestId: UUID) {
		val workManager = WorkManager.getInstance(context)
		workManager.cancelWorkById(requestId)
	}
}
package com.eva.worker.controller

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.eva.worker.STTModelDownloaderWorker
import com.eva.worker.WorkerParams
import java.time.Duration
import java.util.UUID

class STTModelDownloadController(context: Context) : STTWorkerController(context) {

	private val constrains by lazy {
		Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.setRequiresStorageNotLow(true)
			.setRequiresBatteryNotLow(true)
			.build()
	}

	fun startWorker(modelId: String): UUID {

		val workRequest = OneTimeWorkRequestBuilder<STTModelDownloaderWorker>()
			.setInitialDelay(Duration.ofSeconds(1))
			.addTag(WorkerParams.WORKER_TAG_STT_MODEL)
			.setInputData(workDataOf(WorkerParams.WORK_DATA_STT_MODEL_ID to modelId))
			.setConstraints(constrains)
			.build()

		workManager.enqueue(workRequest)

		return workRequest.id
	}

}
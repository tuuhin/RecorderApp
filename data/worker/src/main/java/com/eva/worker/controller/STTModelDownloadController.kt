package com.eva.worker.controller

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.eva.worker.STTModelDownloaderWorker
import com.eva.worker.WorkerParams
import com.eva.worker.domain.WorkResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

	private val workManager by lazy { WorkManager.getInstance(context) }

	fun startWorker(modelId: String): UUID {

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

	fun observeWorker(uuid: UUID): Flow<WorkResource?> {
		return workManager.getWorkInfoByIdFlow(uuid).map { info ->
			if (info == null) return@map null
			when (info.state) {
				WorkInfo.State.ENQUEUED -> WorkResource.Enqueued
				WorkInfo.State.SUCCEEDED -> {
					val message = info.outputData
						.getString(WorkerParams.WORK_DATA_SST_MODEL_WORK_SUCCESS)
					WorkResource.Success(message)
				}

				WorkInfo.State.RUNNING -> {
					val progress = info.progress
						.getInt(WorkerParams.WORK_DATA_WORK_PROGRESS_PERCENTAGE, -1)
					val progressMessage = info.progress
						.getString(WorkerParams.WORK_DATA_WORK_PROGRESS_MESSAGE) ?: ""
					WorkResource.Running(
						message = progressMessage,
						progress = if (progress == -1) null else progress
					)
				}

				WorkInfo.State.FAILED -> {
					val reason = info.outputData
						.getString(WorkerParams.WORK_DATA_SST_MODEL_WORK_FAILED) ?: ""
					WorkResource.Failed(reason)
				}

				WorkInfo.State.BLOCKED -> WorkResource.Blocked
				WorkInfo.State.CANCELLED -> {
					val reason = when (info.stopReason) {
						WorkInfo.STOP_REASON_TIMEOUT -> "Timeout"
						else -> "Worker cancelled for some reason"
					}
					WorkResource.Canceled(reason)
				}
			}
		}
	}

	fun cancelWork(requestId: UUID) {
		workManager.cancelWorkById(requestId)
	}
}
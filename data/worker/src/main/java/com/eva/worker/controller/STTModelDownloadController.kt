package com.eva.worker.controller

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.eva.worker.STTModelDownloaderWorker
import com.eva.worker.WorkerParams
import com.eva.worker.domain.WorkResource
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class STTModelDownloadController(context: Context) : STTWorkerController(context) {

    private val constrains by lazy {
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .setRequiresBatteryNotLow(true)
            .build()
    }

    fun startWorker(modelId: String): Uuid {
        val workRequest = OneTimeWorkRequestBuilder<STTModelDownloaderWorker>()
            .setInitialDelay(200.milliseconds.toJavaDuration())
            .addTag(WorkerParams.WORKER_TAG_STT_MODEL)
            .setInputData(workDataOf(WorkerParams.WORK_DATA_STT_MODEL_ID to modelId))
            .setConstraints(constrains)
            .build()

        workManager.enqueue(workRequest)

        return workRequest.id.toKotlinUuid()
    }

    fun observeDownloadWorkers(): Flow<List<WorkResource>> {
        return workManager.getWorkInfosByTagFlow(WorkerParams.WORKER_TAG_STT_MODEL)
            .map { infos ->
                infos.filterNot { it.state.isFinished }
                    .map(::mapWorkInfo)
            }
    }

}

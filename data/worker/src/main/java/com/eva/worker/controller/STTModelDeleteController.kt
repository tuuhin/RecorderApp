package com.eva.worker.controller

import android.content.Context
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.workDataOf
import com.eva.worker.STTModelDeleteWorker
import com.eva.worker.WorkerParams
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

class STTModelDeleteController(context: Context) : STTWorkerController(context) {

    private val constrains by lazy {
        Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
    }

    fun startWorker(vararg modelIds: String): Uuid {

        val inputData = workDataOf(WorkerParams.WORK_DATA_STT_MODEL_LIST_ID to arrayOf(*modelIds))

        val workRequest = OneTimeWorkRequestBuilder<STTModelDeleteWorker>()
            .addTag(WorkerParams.WORKER_TAG_STT_MODEL)
            .setInputData(inputData)
            .setConstraints(constrains)
            .build()

        workManager.enqueue(workRequest)

        return workRequest.id.toKotlinUuid()
    }

}

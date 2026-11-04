package com.eva.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.eva.transcribe.domain.repository.STTModelsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

@HiltWorker
internal class STTModelDeleteWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val workParams: WorkerParameters,
    private val repository: STTModelsRepository,
) : CoroutineWorker(context, workParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            val modelIds = workParams.inputData
                .getNullableStringArray(WorkerParams.WORK_DATA_STT_MODEL_LIST_ID)
                ?.filterNotNull()
                ?: listOf()

            val deferred = modelIds.map { modelId ->
                async {
                    val modelResult = repository.readModelByLanguage(modelId)
                    val model = modelResult.getOrNull() ?: return@async null
                    repository.deleteSTTModelFile(model)
                }
            }

            val results = deferred.awaitAll().filterNotNull()
            val hasAnyFailed = results.any { it.isFailure }

            // return the success or failure
            if (hasAnyFailed) Result.failure(
                workDataOf(
                    WorkerParams.WORK_DATA_STT_MODEL_LIST_ID to modelIds,
                    WorkerParams.WORK_DATA_SST_MODEL_WORK_FAILED to WorkerParams.WORK_DATA_FAILED_DELETE_MODEL,
                ),
            )
            else Result.success(
                workDataOf(
                    WorkerParams.WORK_DATA_STT_MODEL_LIST_ID to modelIds,
                    WorkerParams.WORK_DATA_SST_MODEL_WORK_SUCCESS to WorkerParams.WORK_DATA_SUCCESS_DELETE_MODEL,
                ),
            )
        }
    }
}

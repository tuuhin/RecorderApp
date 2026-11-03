package com.eva.transcribe.data.repository

import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.eva.database.dao.STTModelsDao
import com.eva.database.entity.STTModelEntity
import com.eva.transcribe.data.toEntity
import com.eva.transcribe.data.toModel
import com.eva.transcribe.domain.exceptions.STTModelEntryUpdateFailedException
import com.eva.transcribe.domain.models.STTModel
import com.eva.transcribe.domain.models.STTModelState
import com.eva.transcribe.domain.repository.STTModelsRepository
import com.eva.utils.Resource
import io.ktor.utils.io.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.IOException

private const val TAG = "STT_MODELS_REPOSITORY"

internal class STTModelsRepositoryImpl(
    private val dao: STTModelsDao,
    private val metaData: STTModelMetadataReader,
) : STTModelsRepository {

    override fun readAllSSTModels(): Flow<Resource<List<STTModel>, Exception>> {
        return channelFlow {
            // a separate launch to set the tone
            launch { syncAssetsToManifest() }

            // now do the read
            dao.readModelsAsFlow()
                .onStart { trySend(Resource.Loading) }
                .catch { err ->
                    if (err is CancellationException) {
                        Log.d(TAG, "FLOW IS CANCELLED")
                    } else if (err is Exception) trySend(Resource.Error(err))
                }
                .collectLatest { entities ->
                    val models = entities.map { it.toModel() }
                    send(Resource.Success(models))
                }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun updateSTTModel(model: STTModel): Result<STTModel> {
        return try {
            dao.updateOrInsertEntity(model.toEntity())
            val readModel = dao.readModelById(model.modelId)
                ?: return Result.failure(STTModelEntryUpdateFailedException())
            Result.success(readModel.toModel())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "SOME ERROR WHILE UPDATE", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteSTTModelFile(model: STTModel): Result<STTModel> {
        return try {
            val path = model.localModelURI
                ?: return Result.failure(Exception("Model is not downloaded"))

            withContext(Dispatchers.IO) {
                Log.d(TAG, "DELETING THE MODEL FOLDER :${model.modelId}")
                val file = path.toUri().toFile()
                val isDeleted = file.deleteRecursively()
                if (!isDeleted) throw IOException("Failed to delete the file")
            }

            val updatedModel =
                model.copy(localModelURI = null, state = STTModelState.UN_AVAILABLE)
                    .toEntity()

            dao.updateOrInsertEntity(updatedModel)
            val readModel = dao.readModelById(model.modelId)
                ?: return Result.failure(STTModelEntryUpdateFailedException())
            Result.success(readModel.toModel())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "SOME ERROR WHILE DELETE", e)
            Result.failure(e)
        }
    }


    override suspend fun readModelByLanguage(modelId: String): Result<STTModel> {
        return try {
            val readModel = dao.readModelById(modelId)
                ?: return Result.failure(STTModelEntryUpdateFailedException())
            Result.success(readModel.toModel())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "SOME ERROR WHILE UPDATE", e)
            Result.failure(e)
        }
    }

    suspend fun syncAssetsToManifest(
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Result<Boolean> {
        val metadataResult = metaData.readMetaData()

        if (metadataResult.isFailure) {
            return Result.failure(
                metadataResult.exceptionOrNull() ?: Exception("Unknown metadata exception"),
            )
        }

        return try {
            val assetsModels = metadataResult.getOrThrow().models
            val dbModels = dao.readAllModelData()
            val dbModelsById = dbModels.associateBy { it.modelId }

            val now = Clock.System.now().toLocalDateTime(timeZone)

            val actions = assetsModels.mapNotNull { metadata ->
                val existing = dbModelsById[metadata.modelId]

                if (existing == null) {
                    // New model: create with default local installation state.
                    metadata.toEntity(timeZone)
                } else {
                    val versionChanged =
                        existing.externalModelVersion != metadata.version

                    val updatedEntity = existing.copy(
                        name = metadata.name,
                        displayName = metadata.displayName,
                        locale = metadata.locale,
                        engine = metadata.engine,
                        externalModelURI = metadata.modelUri,
                        externalModelVersion = metadata.version,
                        archiveSize = metadata.sizeBytes,
                        archiveFormat = metadata.archiveFormat,
                        checksum = metadata.checksum,
                        license = metadata.license,

                        status = when {
                            versionChanged &&
                                existing.status == STTModelEntity.ModelStatus.DOWNLOADED ->
                                STTModelEntity.ModelStatus.UPDATE_AVAILABLE

                            else -> existing.status
                        },
                    )

                    // Avoid unnecessary database updates.
                    if (updatedEntity != existing) {
                        updatedEntity.copy(updatedAt = now)
                    } else {
                        null
                    }
                }
            }

            if (actions.isEmpty()) {
                Log.d(TAG, "MODEL CATALOGUE IS ALREADY UP TO DATE")
                Result.success(false)
            } else {
                Log.d(TAG, "SYNCING ${actions.size} MODEL ENTRIES")

                dao.updateOrInsertModelEntities(actions)

                Result.success(true)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e

            Log.e(TAG, "FAILED TO SYNC MODEL CATALOGUE", e)
            Result.failure(e)
        }
    }
}

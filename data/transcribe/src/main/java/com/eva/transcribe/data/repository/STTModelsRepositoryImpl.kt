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
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import kotlin.time.Clock

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
				val toFilePath = path.toUri().toFile().toOkioPath()
				FileSystem.SYSTEM.delete(toFilePath)
			}

			val updatedModel =
				model.copy(localModelURI = null, state = STTModelState.UN_AVAILABLE, size = 0)
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

	suspend fun syncAssetsToManifest(timeZone: TimeZone = TimeZone.currentSystemDefault()): Result<Unit> {
		val readFiles = metaData.readMetaData()
		if (readFiles.isFailure) {
			val err = readFiles.exceptionOrNull() ?: Exception("Unknown exception")
			return Result.failure(err)
		}
		val assetsModels = readFiles.getOrThrow().models

		return try {
			val dbModels = dao.readAllModelData()

			val actions = assetsModels.mapNotNull { model ->
				val dbEntity = dbModels.find { it.modelId == model.modelId }
				// we have an entry but it's a new version
				if (dbEntity != null && dbEntity.externalModelVersion != model.version) {
					dbEntity.copy(
						status = STTModelEntity.ModelStatus.UPDATE_AVAILABLE,
						externalModelURI = model.modelUri,
						externalModelVersion = model.version,
						version = dbEntity.version + 1,
						updatedAt = Clock.System.now().toLocalDateTime(timeZone)
					)
				}
				// db entity don't exist thus add a new entry
				else if (dbEntity == null) model.toEntity(timeZone)
				// already present and no version is updated
				else null
			}

			// update or insert entities
			if (actions.isNotEmpty())
				dao.updateOrInsertModelEntities(actions)

			Result.success(Unit)
		} catch (e: Exception) {
			if (e is CancellationException) throw e
			Result.failure(e)
		}
	}
}
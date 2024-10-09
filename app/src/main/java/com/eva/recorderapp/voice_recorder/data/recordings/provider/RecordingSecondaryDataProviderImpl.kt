package com.eva.recorderapp.voice_recorder.data.recordings.provider

import android.content.Context
import android.database.sqlite.SQLiteException
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.database.dao.RecordingsMetadataDao
import com.eva.recorderapp.voice_recorder.data.database.entity.RecordingsMetaDataEntity
import com.eva.recorderapp.voice_recorder.data.recordings.utils.toMetadataEntity
import com.eva.recorderapp.voice_recorder.data.recordings.utils.toModel
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import com.eva.recorderapp.voice_recorder.domain.recordings.exceptions.InvalidRecordingIdException
import com.eva.recorderapp.voice_recorder.domain.recordings.models.ExtraRecordingMetadataModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ExtraRecordingMetaDataList
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RecordingSecondaryDataProviderImpl(
	private val context: Context,
	private val recordingsDao: RecordingsMetadataDao,
) : RecordingsSecondaryDataProvider {

	override val providesRecordingMetaData: Flow<ExtraRecordingMetaDataList>
		get() = recordingsDao.getAllRecordingsMetaDataAsFlow()
			.flowOn(Dispatchers.IO)
			.map { entries -> entries.map(RecordingsMetaDataEntity::toModel) }


	override fun recordingsFromCategory(category: RecordingCategoryModel): Flow<List<ExtraRecordingMetadataModel>> {
		return recordingsDao.getRecordingsFromCategoryIdAsFlow(category.id)
			.flowOn(Dispatchers.IO)
			.map { entries -> entries.map(RecordingsMetaDataEntity::toModel) }
	}

	override fun getRecordingFromIdAsFlow(recordingId: Long): Flow<ExtraRecordingMetadataModel?> {
		return recordingsDao.getRecordingMetaDataFromIdAsFlow(recordingId)
			.flowOn(Dispatchers.IO)
			.map { entity -> entity?.toModel() }
	}

	override suspend fun insertRecordingMetaData(recordingId: Long): Resource<ExtraRecordingMetadataModel, Exception> {
		return try {
			val result = withContext(Dispatchers.IO) {
				val entityId = async {
					val entity = RecordingsMetaDataEntity(recordingId = recordingId)
					// creates the entry
					recordingsDao.updateOrInsertRecordingMetadata(entity)
				}
				//get the value
				recordingsDao.getRecordingMetaDataFromId(entityId.await())
			}
			result?.let { entity ->
				Resource.Success(entity.toModel())
			} ?: Resource.Error(InvalidRecordingIdException())
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}

	override suspend fun insertRecordingsMetaDataBulk(recordingsIds: List<Long>): Resource<Boolean, Exception> {
		return try {
			withContext(Dispatchers.IO) {
				val entities = recordingsIds.map { RecordingsMetaDataEntity(recordingId = it) }
				recordingsDao.updateOrInsertRecordingMetadataBulk(entities)
			}
			Resource.Success(false)
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}

	override suspend fun updateRecordingMetaData(model: RecordedVoiceModel): Resource<ExtraRecordingMetadataModel, Exception> {
		return try {
			val result = withContext(Dispatchers.IO) {
				// fetch the old data from id
				val entityFromId = recordingsDao.getRecordingMetaDataFromId(model.id)
					?: return@withContext null
				//update the contents
				val updatedEntity = entityFromId.copy(isFavourite = model.isFavorite)
				// update the entities
				val entityId = async {
					recordingsDao.updateOrInsertRecordingMetadata(updatedEntity)
				}
				//get the value
				recordingsDao.getRecordingMetaDataFromId(entityId.await())
			}
			result?.let { entity ->
				Resource.Success(
					data = entity.toModel(),
					message = context.getString(R.string.categories_updated)
				)
			} ?: Resource.Error(InvalidRecordingIdException())
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}

	override suspend fun updateRecordingCategoryBulk(
		recordingIds: List<Long>,
		category: RecordingCategoryModel,
	): Resource<Boolean, Exception> {
		return try {
			withContext(Dispatchers.IO) {
				val categoryId = if (category != RecordingCategoryModel.ALL_CATEGORY)
					category.id else null

				// fetch the entities from ids
				val entities = recordingsDao.getRecordingMetaDataFromIds(recordingIds)
				//update the contents
				val updatedEntities = entities.map { entity ->
					entity.copy(categoryId = categoryId)
				}

				val entitiesId = entities.map { it.recordingId }
				// create new metadata as entry was not preset
				val notFoundIds = recordingIds.filterNot { it in entitiesId }
				val newlyAdded = notFoundIds.map { id ->
					RecordingsMetaDataEntity(recordingId = id, categoryId = categoryId)
				}
				// update the entities
				recordingsDao.updateOrInsertRecordingMetadataBulk(updatedEntities + newlyAdded)

				val message = context.getString(R.string.categories_updated, category.name)
				Resource.Success(data = true, message = message)
			}
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}

	override suspend fun favouriteRecordingsBulk(
		models: VoiceRecordingModels,
		isFavourite: Boolean,
	): Resource<Unit, Exception> {
		return try {
			val recordingIds = models.map { it.id }

			withContext(Dispatchers.IO) {
				// fetch the entities from ids
				val entitiesFromIds = recordingsDao.getRecordingMetaDataFromIds(recordingIds)
				//update the contents
				val entities = entitiesFromIds.map { entity ->
					entity.copy(isFavourite = isFavourite)
				}
				val entitiesId = entities.map { it.recordingId }
				// create new metadata as entry was not preset
				val notFoundIds = recordingIds.filterNot { it in entitiesId }
				val newlyAdded = notFoundIds.map { id ->
					RecordingsMetaDataEntity(recordingId = id, isFavourite = isFavourite)
				}
				// update the entities
				recordingsDao.updateOrInsertRecordingMetadataBulk(entities + newlyAdded)
			}

			val message = if (isFavourite)
				context.getString(R.string.recordings_add_to_favourite_success)
			else context.getString(R.string.recordings_remove_favourite_failed)

			Resource.Success(data = Unit, message = message)
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}

	override suspend fun deleteRecordingMetaDataBulk(models: VoiceRecordingModels): Resource<Boolean, Exception> {
		return try {
			withContext(Dispatchers.IO) {
				val entities = models.map(RecordedVoiceModel::toMetadataEntity)
				recordingsDao.deleteRecordingsMetaDataBulk(entities)
			}
			Resource.Success(false)
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}

	override suspend fun favouriteAudioFile(
		file: AudioFileModel,
		isFav: Boolean,
	): Resource<Unit, Exception> {
		return try {
			withContext(Dispatchers.IO) {
				val recordingId = file.id
				val entity = recordingsDao.getRecordingMetaDataFromId(recordingId)
					?: RecordingsMetaDataEntity(recordingId = recordingId)

				val updated = entity.copy(isFavourite = isFav)
				recordingsDao.updateOrInsertRecordingMetadata(updated)
			}
			val message = if (isFav)
				context.getString(R.string.recordings_add_to_favourite_success)
			else context.getString(R.string.recordings_remove_favourite_failed)

			Resource.Success(Unit, message = message)
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}
}
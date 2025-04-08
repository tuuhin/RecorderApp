package com.eva.recordings.data.provider

import android.content.Context
import android.database.sqlite.SQLiteException
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.database.dao.RecordingsMetadataDao
import com.eva.database.entity.RecordingsMetaDataEntity
import com.eva.recordings.R
import com.eva.recordings.data.utils.toMetadataEntity
import com.eva.recordings.data.utils.toModel
import com.eva.recordings.domain.exceptions.InvalidRecordingIdException
import com.eva.recordings.domain.models.AudioFileModel
import com.eva.recordings.domain.models.ExtraRecordingMetadataModel
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.provider.ExtraRecordingMetaDataList
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.recordings.domain.provider.VoiceRecordingModels
import com.eva.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class RecordingSecondaryDataProviderImpl(
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
			.map { entity -> entity?.toModel() }
	}


	override suspend fun checkRecordingIdExists(recordingId: Long): Boolean? {
		return withContext(Dispatchers.IO) {
			try {
				val result = recordingsDao.checkRecordingWithIdExists(recordingId)
				result == 1
			} catch (e: Exception) {
				e.printStackTrace()
				null
			}
		}
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
				val entriesFromId = recordingsDao.getRecordingMetaDataFromIds(recordingIds)
				//update the contents
				val updatedEntities = entriesFromId.map { entity ->
					entity.copy(categoryId = categoryId)
				}

				val entitiesId = entriesFromId.map { it.recordingId }
				// create new metadata as entry was not preset
				val notFoundIds = recordingIds.filterNot { it in entitiesId }
				val newlyAdded = notFoundIds.map { id ->
					RecordingsMetaDataEntity(recordingId = id, categoryId = categoryId)
				}
				// update the entities
				val allEntries = (updatedEntities + newlyAdded).distinctBy { it.recordingId }
				recordingsDao.updateOrInsertRecordingMetadataBulk(allEntries)

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


	override suspend fun favouriteRecordingsBulk(models: VoiceRecordingModels, isFavourite: Boolean)
			: Resource<Unit, Exception> {
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
				val allEntries = (entities + newlyAdded).distinctBy { it.recordingId }
				// update the entities
				recordingsDao.updateOrInsertRecordingMetadataBulk(allEntries)
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


	override suspend fun deleteRecordingMetaDataBulk(models: VoiceRecordingModels): Resource<Unit, Exception> {
		return try {
			withContext(Dispatchers.IO) {
				val entities = models.map(RecordedVoiceModel::toMetadataEntity)
				recordingsDao.deleteRecordingsMetaDataBulk(entities)
			}
			Resource.Success(Unit)
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, e.message ?: "")
		}
	}


	override suspend fun favouriteAudioFile(file: AudioFileModel, isFav: Boolean)
			: Resource<Unit, Exception> {
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
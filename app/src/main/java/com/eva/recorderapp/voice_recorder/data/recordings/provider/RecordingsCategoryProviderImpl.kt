package com.eva.recorderapp.voice_recorder.data.recordings.provider

import android.database.sqlite.SQLiteException
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.recordings.database.dao.RecordingCategoryDao
import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingCategoryEntity
import com.eva.recorderapp.voice_recorder.data.recordings.utils.toEntity
import com.eva.recorderapp.voice_recorder.data.recordings.utils.toModel
import com.eva.recorderapp.voice_recorder.domain.recordings.exceptions.RecordingCategoryNotFound
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingCategoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class RecordingsCategoryProviderImpl(
	private val categoryDao: RecordingCategoryDao
) : RecordingCategoryProvider {

	private val localtimeNow: LocalDateTime
		get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

	override val recordingCategoryFlow: Flow<Resource<List<RecordingCategoryModel>, Exception>>
		get() = flow {
			try {
				val result = categoryDao.getAllCategoryAsFlow()
					.flowOn(Dispatchers.IO)
					.map { entries ->
						val result = entries.map(RecordingCategoryEntity::toModel)
						Resource.Success<List<RecordingCategoryModel>, Nothing>(result)
					}
				emitAll(result)
			} catch (e: SQLiteException) {
				emit(Resource.Error(e, "SQL EXCEPTION"))
			} catch (e: Exception) {
				e.printStackTrace()
				emit(Resource.Error(e, e.message ?: ""))
			}
		}

	override suspend fun createCategory(name: String): Resource<RecordingCategoryModel, Exception> {
		return try {
			val result = withContext(Dispatchers.IO) {
				val entityId = async {
					val entity = RecordingCategoryEntity(
						categoryName = name,
						createdAt = localtimeNow
					)
					// creates the entry
					categoryDao.updateOrInsertCategory(entity)
				}
				//get the value
				categoryDao.getEntityFromId(entityId.await())
			}
			return result?.let { entity -> Resource.Success(entity.toModel()) }
				?: Resource.Error(RecordingCategoryNotFound())
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

	override suspend fun updateCategory(category: RecordingCategoryModel): Resource<RecordingCategoryModel, Exception> {
		return try {
			val result = withContext(Dispatchers.IO) {
				// update it
				categoryDao.updateOrInsertCategory(entity = category.toEntity())
				//get the value
				categoryDao.getEntityFromId(id = category.id)
			}
			return result?.let { entity -> Resource.Success(entity.toModel()) }
				?: Resource.Error(RecordingCategoryNotFound())
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

	override suspend fun deleteCategory(category: RecordingCategoryModel): Resource<Boolean, Exception> {
		return try {
			withContext(Dispatchers.IO) {
				categoryDao.deleteCategory(entity = category.toEntity())
			}
			Resource.Success(true)
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

	override suspend fun deleteCategories(categories: Collection<RecordingCategoryModel>): Resource<Boolean, Exception> {
		return try {
			withContext(Dispatchers.IO) {
				val entities = categories.map(RecordingCategoryModel::toEntity)
				categoryDao.deleteCategories(entities = entities)
			}
			Resource.Success(true)
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}
}
package com.eva.recorderapp.voice_recorder.data.recordings.provider

import android.content.Context
import android.database.sqlite.SQLiteException
import com.eva.recorderapp.R
import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.data.recordings.database.dao.RecordingCategoryDao
import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingCategoryEntity
import com.eva.recorderapp.voice_recorder.data.recordings.utils.toEntity
import com.eva.recorderapp.voice_recorder.data.recordings.utils.toModel
import com.eva.recorderapp.voice_recorder.domain.categories.exceptions.RecordingCategoryNotFoundException
import com.eva.recorderapp.voice_recorder.domain.categories.exceptions.UnmodifiableRecordingCategoryException
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryColor
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryType
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingCategoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime as JLocalDateTime

class RecordingsCategoryProviderImpl(
	private val context: Context,
	private val categoryDao: RecordingCategoryDao,
) : RecordingCategoryProvider {

	private val localtimeNow: LocalDateTime
		get() = JLocalDateTime.now().toKotlinLocalDateTime()

	override val recordingCategoryAsResourceFlow: Flow<Resource<List<RecordingCategoryModel>, Exception>>
		get() = flow {
			try {
				val result = categoryDao.getAllCategoryAsFlow()
					.flowOn(Dispatchers.IO)
					.map { entries ->
						val result = buildList {
							add(RecordingCategoryModel.ALL_CATEGORY)
							addAll(entries.map(RecordingCategoryEntity::toModel))
						}
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

	override suspend fun getCategoryFromId(id: Long): Resource<RecordingCategoryModel, Exception> {
		return try {
			val result = withContext(Dispatchers.IO) {
				//get the value
				categoryDao.getCategoryFromId(id = id)
			}
			return result?.let { entity -> Resource.Success(entity.toModel()) }
				?: Resource.Error(RecordingCategoryNotFoundException())
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

	override val recordingCategoriesFlowWithItemCount: Flow<Resource<List<RecordingCategoryModel>, Exception>>
		get() = flow {
			try {
				val result = categoryDao.getCategoriesAsFlowWithCount()
					.flowOn(Dispatchers.IO)
					.map { entries ->
						val models = entries.map { (entity, count) -> entity.toModel(count) }
						val result = buildList {
							add(RecordingCategoryModel.ALL_CATEGORY)
							addAll(models)
						}
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
					categoryDao.insertOrUpdateCategory(entity)
				}
				//get the value
				categoryDao.getCategoryFromId(entityId.await())
			}
			val message = context.getString(R.string.categories_create_success)
			return result?.let { entity -> Resource.Success(entity.toModel(), message = message) }
				?: Resource.Error(RecordingCategoryNotFoundException())
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

	override suspend fun createCategory(name: String, color: CategoryColor, type: CategoryType)
			: Resource<RecordingCategoryModel, Exception> {
		return try {
			val result = withContext(Dispatchers.IO) {
				val entityId = async {
					val entity = RecordingCategoryEntity(
						categoryName = name,
						createdAt = localtimeNow,
						color = color,
						type = type
					)
					// creates the entry
					categoryDao.insertOrUpdateCategory(entity)
				}
				//get the value
				categoryDao.getCategoryFromId(entityId.await())
			}
			val message = context.getString(R.string.categories_create_success)
			return result?.let { entity -> Resource.Success(entity.toModel(), message = message) }
				?: Resource.Error(RecordingCategoryNotFoundException())
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}


	override suspend fun updateCategory(category: RecordingCategoryModel): Resource<RecordingCategoryModel, Exception> {
		return try {
			if (category == RecordingCategoryModel.ALL_CATEGORY)
				return Resource.Error(UnmodifiableRecordingCategoryException())
			val result = withContext(Dispatchers.IO) {
				// update it or insert it
				val id = async { categoryDao.insertOrUpdateCategory(entity = category.toEntity()) }
				//get the value
				categoryDao.getCategoryFromId(id = id.await())
			}
			val message = context.getString(R.string.categories_updated)
			return result?.let { entity -> Resource.Success(entity.toModel(), message) }
				?: Resource.Error(RecordingCategoryNotFoundException())
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, message = e.message ?: "Some Excetpion")
		}
	}

	override suspend fun deleteCategory(category: RecordingCategoryModel): Resource<Boolean, Exception> {
		return try {
			if (category == RecordingCategoryModel.ALL_CATEGORY)
				return Resource.Error(UnmodifiableRecordingCategoryException())
			withContext(Dispatchers.IO) {
				categoryDao.deleteCategory(entity = category.toEntity())
			}
			val message = context.getString(R.string.categories_deleted)
			Resource.Success(true, message = message)
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

				val entities = categories
					.filter { it != RecordingCategoryModel.ALL_CATEGORY }
					.map(RecordingCategoryModel::toEntity)

				categoryDao.deleteCategoriesBulk(entities = entities)
			}
			val message = context.getString(R.string.categories_deleted)
			Resource.Success(true, message = message)
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}
}
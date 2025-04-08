package com.eva.categories.data

import android.content.Context
import android.database.sqlite.SQLiteException
import com.eva.categories.R
import com.eva.categories.domain.exceptions.RecordingCategoryNotFoundException
import com.eva.categories.domain.exceptions.UnModifiableRecordingCategoryException
import com.eva.categories.domain.models.CategoryColor
import com.eva.categories.domain.models.CategoryType
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.categories.domain.provider.RecordingCategoriesModels
import com.eva.categories.domain.provider.RecordingCategoryProvider
import com.eva.database.dao.RecordingCategoryDao
import com.eva.database.entity.RecordingCategoryEntity
import com.eva.utils.Resource
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

internal class RecordingsCategoryProviderImpl(
	private val context: Context,
	private val categoryDao: RecordingCategoryDao,
) : RecordingCategoryProvider {

	private val localtimeNow: LocalDateTime
		get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

	override val recordingCategoryAsResourceFlow: Flow<Resource<RecordingCategoriesModels, Exception>>
		get() = flow {
			try {
				val result = categoryDao.getAllCategoryAsFlow()
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
						color = color.name,
						type = type.name
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
				return Resource.Error(UnModifiableRecordingCategoryException())
			val result = withContext(Dispatchers.IO) {
				// update it or insert it
				categoryDao.insertOrUpdateCategory(entity = category.toEntity())
				//get the value
				categoryDao.getCategoryFromId(id = category.id)
			}
			return result?.let { entity ->
				val message = context.getString(R.string.categories_updated, entity.categoryName)
				Resource.Success(entity.toModel(), message)
			} ?: Resource.Error(RecordingCategoryNotFoundException())
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, message = e.message ?: "Some Exception")
		}
	}

	override suspend fun deleteCategory(category: RecordingCategoryModel): Resource<Boolean, Exception> {
		return try {
			if (category == RecordingCategoryModel.ALL_CATEGORY)
				return Resource.Error(UnModifiableRecordingCategoryException())
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
					.map { it.toEntity() }

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
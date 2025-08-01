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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class RecordingsCategoryProviderImpl(
	private val context: Context,
	private val categoryDao: RecordingCategoryDao,
) : RecordingCategoryProvider {

	@OptIn(ExperimentalTime::class)
	private val localtimeNow: LocalDateTime
		get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

	override val recordingCategoryAsResourceFlow: Flow<Resource<RecordingCategoriesModels, Exception>>
		get() = categoryDao.getAllCategoryAsFlow()
			.map { entries ->
				val result = buildList {
					add(RecordingCategoryModel.ALL_CATEGORY)
					addAll(entries.map(RecordingCategoryEntity::toModel))
				}
				Resource.Success<List<RecordingCategoryModel>, Exception>(result)
						as Resource<List<RecordingCategoryModel>, Exception>
			}
			.onStart { emit(Resource.Loading) }
			.catch { err ->
				if (err is SQLiteException)
					emit(Resource.Error(err, "SQL EXCEPTION"))
				else {
					err.printStackTrace()
					emit(Resource.Error(Exception(err), err.message ?: ""))
				}
			}


	override suspend fun getCategoryFromId(id: Long): Resource<RecordingCategoryModel, Exception> {
		return try {
			//get the value
			val result = categoryDao.getCategoryFromId(id = id)

			return result?.let { entity -> Resource.Success(entity.toModel()) }
				?: Resource.Error(RecordingCategoryNotFoundException())
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

	override suspend fun createCategory(name: String): Resource<RecordingCategoryModel, Exception> {
		return try {
			val entity = RecordingCategoryEntity(
				categoryName = name,
				createdAt = localtimeNow
			)
			// creates the entry
			val entityId = categoryDao.insertOrUpdateCategory(entity)
			//get the value
			val result = categoryDao.getCategoryFromId(entityId)

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

			val entity = RecordingCategoryEntity(
				categoryName = name,
				createdAt = localtimeNow,
				color = color.name,
				type = type.name
			)
			// creates the entry
			val entityId = categoryDao.insertOrUpdateCategory(entity)

			//get the value
			val result = categoryDao.getCategoryFromId(entityId)
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
			// update it or insert it
			categoryDao.insertOrUpdateCategory(entity = category.toEntity())
			//get the value
			val result = categoryDao.getCategoryFromId(id = category.id)
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

			categoryDao.deleteCategory(entity = category.toEntity())

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
			val entities = categories
				.filter { it != RecordingCategoryModel.ALL_CATEGORY }
				.map { it.toEntity() }

			categoryDao.deleteCategoriesBulk(entities = entities)
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
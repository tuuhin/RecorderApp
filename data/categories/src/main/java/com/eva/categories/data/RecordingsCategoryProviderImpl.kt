package com.eva.categories.data

import android.content.Context
import android.database.sqlite.SQLiteException
import com.eva.categories.domain.exceptions.RecordingCategoryNotFoundException
import com.eva.categories.domain.exceptions.UnModifiableRecordingCategoryException
import com.eva.categories.domain.models.CategoryColor
import com.eva.categories.domain.models.CategoryType
import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.categories.domain.provider.RecordingCategoriesModels
import com.eva.categories.domain.provider.RecordingCategoryProvider
import com.eva.database.dao.RecordingCategoryDao
import com.eva.database.entity.RecordingCategoryEntity
import com.eva.ui.R
import com.eva.utils.Resource
import kotlinx.coroutines.CancellationException
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
			.map { entities ->
				val categories = buildList {
					add(RecordingCategoryModel.ALL_CATEGORY)
					addAll(entities.map(RecordingCategoryEntity::toModel))
				}
				Resource.Success<RecordingCategoriesModels, Exception>(categories) as Resource<RecordingCategoriesModels, Exception>
			}
			.onStart { emit(Resource.Loading) }
			.catch { error ->
				error.printStackTrace()
				val message = if (error is SQLiteException) "SQL EXCEPTION"
				else error.message ?: "Unknown error occurred"
				emit(Resource.Error(Exception(error), message))
			}

	override suspend fun getCategoryFromId(id: Long): Resource<RecordingCategoryModel, Exception> {
		return try {
			//get the value
			val result = categoryDao.getCategoryFromId(id = id)
				?: return Resource.Error(RecordingCategoryNotFoundException())

			return Resource.Success(result.toModel())
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
				createdAt = localtimeNow,
			)
			// creates the entry
			val entityId = categoryDao.insertOrUpdateCategory(entity)
			//get the value
			val result = categoryDao.getCategoryFromId(entityId)
				?: return Resource.Error(RecordingCategoryNotFoundException())

			val message = context.getString(R.string.categories_create_success)
			return Resource.Success(result.toModel(), message = message)
		} catch (e: CancellationException) {
			throw e
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}

	override suspend fun createCategory(
		name: String,
		color: CategoryColor,
		type: CategoryType
	): Resource<RecordingCategoryModel, Exception> {
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
				?: return Resource.Error(RecordingCategoryNotFoundException())

			val message = context.getString(R.string.categories_create_success)
			return Resource.Success(result.toModel(), message = message)
		} catch (e: CancellationException) {
			throw e
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}


	override suspend fun updateCategory(category: RecordingCategoryModel): Resource<RecordingCategoryModel, Exception> {
		if (category == RecordingCategoryModel.ALL_CATEGORY)
			return Resource.Error(UnModifiableRecordingCategoryException())
		return try {
			// check if exists is not return absent
			categoryDao.getCategoryFromId(category.id)
				?: return Resource.Error(RecordingCategoryNotFoundException())
			// update this
			categoryDao.insertOrUpdateCategory(entity = category.toEntity())
			//get the value
			val entity = categoryDao.getCategoryFromId(id = category.id)
				?: return Resource.Error(RecordingCategoryNotFoundException())

			val message = context.getString(R.string.categories_updated, entity.categoryName)
			return Resource.Success(entity.toModel(), message)

		} catch (e: CancellationException) {
			throw e
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e, message = e.message ?: "Some Exception")
		}
	}

	override suspend fun deleteCategory(category: RecordingCategoryModel): Resource<Boolean, Exception> {
		if (category == RecordingCategoryModel.ALL_CATEGORY)
			return Resource.Error(UnModifiableRecordingCategoryException())

		return try {
			categoryDao.deleteCategory(entity = category.toEntity())
			val message = context.getString(R.string.categories_deleted)
			Resource.Success(true, message = message)
		} catch (e: CancellationException) {
			throw e
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
		} catch (e: CancellationException) {
			throw e
		} catch (e: SQLiteException) {
			Resource.Error(e, "SQL EXCEPTION")
		} catch (e: Exception) {
			e.printStackTrace()
			Resource.Error(e)
		}
	}
}
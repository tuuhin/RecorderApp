package com.eva.recorderapp.voice_recorder.data.recordings.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.eva.recorderapp.voice_recorder.data.recordings.database.entity.RecordingCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingCategoryDao {

	@Upsert
	suspend fun updateOrInsertCategory(entity: RecordingCategoryEntity): Long

	@Query("SELECT * FROM RECORDINGS_CATEGORY WHERE CATEGORY_ID=:id")
	suspend fun getEntityFromId(id:Long):RecordingCategoryEntity?

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addCategories(entities: Collection<RecordingCategoryEntity>)

	@Query("SELECT * FROM recordings_category")
	fun getAllCategoryAsFlow(): Flow<List<RecordingCategoryEntity>>

	@Query("SELECT * FROM recordings_category")
	fun getAllCategoryAsList(): List<RecordingCategoryEntity>

	@Delete
	suspend fun deleteCategory(entity: RecordingCategoryEntity)

	@Delete
	suspend fun deleteCategories(entities: Collection<RecordingCategoryEntity>)
}
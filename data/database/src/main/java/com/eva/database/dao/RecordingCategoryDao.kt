package com.eva.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapColumn
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.eva.database.entity.RecordingCategoryEntity
import kotlinx.coroutines.flow.Flow

typealias RecordingsCategoryEntitiesWithCount
		= Map<RecordingCategoryEntity, @MapColumn("R_COUNT") Long>

@Dao
interface RecordingCategoryDao {

	@Upsert
	suspend fun insertOrUpdateCategory(entity: RecordingCategoryEntity): Long

	@Insert(onConflict = OnConflictStrategy.IGNORE)
	suspend fun addCategoriesBulk(entities: Collection<RecordingCategoryEntity>)

	@Query("SELECT * FROM RECORDINGS_CATEGORY WHERE CATEGORY_ID=:id")
	suspend fun getCategoryFromId(id: Long): RecordingCategoryEntity?

	@Query("SELECT * FROM RECORDINGS_CATEGORY")
	fun getAllCategoryAsFlow(): Flow<List<RecordingCategoryEntity>>

	@Query("SELECT * FROM RECORDINGS_CATEGORY")
	fun getAllCategoryAsList(): List<RecordingCategoryEntity>

	@Query(
		"""
		SELECT RECORDINGS_CATEGORY.* , COUNT(RECORDING_META_DATA.RECORDING_ID) AS R_COUNT
		FROM RECORDINGS_CATEGORY 
		LEFT JOIN RECORDING_META_DATA 
		ON RECORDINGS_CATEGORY.CATEGORY_ID = RECORDING_META_DATA.CATEGORY_ID
		GROUP BY RECORDINGS_CATEGORY.CATEGORY_ID
		"""
	)
	fun getCategoriesAsFlowWithCount(): Flow<RecordingsCategoryEntitiesWithCount>

	@Delete
	suspend fun deleteCategory(entity: RecordingCategoryEntity)

	@Delete
	suspend fun deleteCategoriesBulk(entities: Collection<RecordingCategoryEntity>)
}
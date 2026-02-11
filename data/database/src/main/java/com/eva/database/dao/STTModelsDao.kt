package com.eva.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.eva.database.entity.STTModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface STTModelsDao {

	@Upsert
	suspend fun updateOrInsertEntity(entity: STTModelEntity): Long

	@Upsert
	suspend fun updateOrInsertModelEntities(entities: List<STTModelEntity>)

	@Query("SELECT * FROM stt_models_table WHERE _id=:modelId")
	suspend fun readModelById(modelId: String): STTModelEntity?

	@Query("SELECT * FROM stt_models_table")
	suspend fun readAllModelData(): List<STTModelEntity>

	@Query("SELECT * FROM stt_models_table")
	fun readModelsAsFlow(): Flow<List<STTModelEntity>>

	@Delete
	suspend fun deleteModelEntity(entity: STTModelEntity)
}
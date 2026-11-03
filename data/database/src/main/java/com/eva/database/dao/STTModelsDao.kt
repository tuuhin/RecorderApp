package com.eva.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Upsert
import com.eva.database.DataBaseConstants
import com.eva.database.entity.STTModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface STTModelsDao {

    @Upsert
    suspend fun updateOrInsertEntity(entity: STTModelEntity)

    @Upsert
    suspend fun updateOrInsertModelEntities(entities: List<STTModelEntity>)

    @Query("SELECT * FROM ${DataBaseConstants.STT_MODELS_TABLE} WHERE _id = :modelId")
    suspend fun readModelById(modelId: String): STTModelEntity?

    @Query("SELECT * FROM ${DataBaseConstants.STT_MODELS_TABLE}")
    suspend fun readAllModelData(): List<STTModelEntity>

    @Query("SELECT * FROM ${DataBaseConstants.STT_MODELS_TABLE}")
    fun readModelsAsFlow(): Flow<List<STTModelEntity>>

    @Delete
    suspend fun deleteModelEntity(entity: STTModelEntity)

    @Query("DELETE FROM ${DataBaseConstants.STT_MODELS_TABLE} WHERE _id = :modelId")
    suspend fun deleteModelById(modelId: String)
}

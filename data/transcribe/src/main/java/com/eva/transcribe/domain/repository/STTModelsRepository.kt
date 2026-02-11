package com.eva.transcribe.domain.repository

import com.eva.transcribe.domain.models.STTModel
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow

interface STTModelsRepository {

	fun readAllSSTModels(): Flow<Resource<List<STTModel>, Exception>>

	suspend fun updateSTTModel(model: STTModel): Result<STTModel>

	suspend fun deleteSTTModelFile(model: STTModel): Result<STTModel>

	suspend fun readModelByLanguage(modelId: String): Result<STTModel>
}
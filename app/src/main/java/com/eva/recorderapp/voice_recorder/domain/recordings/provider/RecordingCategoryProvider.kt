package com.eva.recorderapp.voice_recorder.domain.recordings.provider

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel
import kotlinx.coroutines.flow.Flow

interface RecordingCategoryProvider {

	val recordingCategoryFlow: Flow<Resource<List<RecordingCategoryModel>, Exception>>

	suspend fun createCategory(name: String): Resource<RecordingCategoryModel, Exception>


	suspend fun updateCategory(category: RecordingCategoryModel): Resource<RecordingCategoryModel, Exception>

	suspend fun deleteCategory(category: RecordingCategoryModel): Resource<Boolean, Exception>

	suspend fun deleteCategories(categories: Collection<RecordingCategoryModel>): Resource<Boolean, Exception>

}
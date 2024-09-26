package com.eva.recorderapp.voice_recorder.domain.recordings.provider

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryColor
import com.eva.recorderapp.voice_recorder.domain.categories.models.CategoryType
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import kotlinx.coroutines.flow.Flow

typealias RecordingCategoriesModels = List<RecordingCategoryModel>


interface RecordingCategoryProvider {

	val recordingCategoryAsResourceFlow: Flow<Resource<RecordingCategoriesModels, Exception>>

	val recordingCategoriesFlowWithItemCount: Flow<Resource<RecordingCategoriesModels, Exception>>

	suspend fun getCategoryFromId(id: Long): Resource<RecordingCategoryModel, Exception>

	suspend fun createCategory(name: String): Resource<RecordingCategoryModel, Exception>

	suspend fun createCategory(
		name: String,
		color: CategoryColor,
		type: CategoryType,
	): Resource<RecordingCategoryModel, Exception>

	suspend fun updateCategory(category: RecordingCategoryModel): Resource<RecordingCategoryModel, Exception>

	suspend fun deleteCategory(category: RecordingCategoryModel): Resource<Boolean, Exception>

	suspend fun deleteCategories(categories: Collection<RecordingCategoryModel>): Resource<Boolean, Exception>
}
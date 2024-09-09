package com.eva.recorderapp.voice_recorder.domain.recordings.provider

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.models.ExtraRecordingMetadataModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel
import kotlinx.coroutines.flow.Flow

typealias RecordingCategoriesModels = List<RecordingCategoryModel>
typealias ExtraRecordingMetaDataList = List<ExtraRecordingMetadataModel>

interface RecordingCategoryProvider {

	val recordingCategoryAsResourceFlow: Flow<Resource<RecordingCategoriesModels, Exception>>

	val recordingCategoriesFlowWithItemCount: Flow<Resource<RecordingCategoriesModels, Exception>>

	val providesRecordingMetaData: Flow<ExtraRecordingMetaDataList>

	fun recordingsFromCategory(category: RecordingCategoryModel): Flow<ExtraRecordingMetaDataList>

	suspend fun createCategory(name: String): Resource<RecordingCategoryModel, Exception>

	suspend fun updateCategory(category: RecordingCategoryModel): Resource<RecordingCategoryModel, Exception>

	suspend fun deleteCategory(category: RecordingCategoryModel): Resource<Boolean, Exception>

	suspend fun deleteCategories(categories: Collection<RecordingCategoryModel>): Resource<Boolean, Exception>
}
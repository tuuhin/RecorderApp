package com.eva.recorderapp.voice_recorder.domain.use_cases

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ExtraRecordingMetaDataList
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingCategoryProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ResourcedVoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RecordingsFromCategoriesUseCase(
	private val recordings: VoiceRecordingsProvider,
	private val categories: RecordingCategoryProvider,
) {
	operator fun invoke(categoryModel: RecordingCategoryModel? = null): Flow<ResourcedVoiceRecordingModels> {
		return if (categoryModel == null || categoryModel == RecordingCategoryModel.ALL_CATEGORY) {
			combine(
				flow = recordings.voiceRecordingsFlow,
				flow2 = categories.providesRecordingMetaData
			) { originalMetaData, extraMetaData ->
				try {
					val result = mergePrimaryAndSecondaryMetadata(originalMetaData, extraMetaData)
					Resource.Success(result)
				} catch (e: Exception) {
					e.printStackTrace()
					Resource.Error(e)
				}
			}
		} else {
			val extraMetaDataFlow = categories.recordingsFromCategory(categoryModel)
			combine(
				flow = recordings.voiceRecordingsFlow,
				flow2 = extraMetaDataFlow
			) { originalMetaData, extraMetaData ->
				try {
					val matchingIds = extraMetaData.map { it.recordingId }
					val selectedData = originalMetaData.filter { it.id in matchingIds }
					val result = mergePrimaryAndSecondaryMetadata(selectedData, extraMetaData)
					Resource.Success(result)
				} catch (e: Exception) {
					e.printStackTrace()
					Resource.Error(e)
				}
			}
		}
	}

	private fun mergePrimaryAndSecondaryMetadata(
		recordings: VoiceRecordingModels,
		otherMetadata: ExtraRecordingMetaDataList,
	): VoiceRecordingModels {
		return recordings.map { model ->
			val hasExtraData = otherMetadata.find { it.recordingId == model.id } ?: return@map model
			model.copy(isFavorite = hasExtraData.isFavourite)
		}
	}

}
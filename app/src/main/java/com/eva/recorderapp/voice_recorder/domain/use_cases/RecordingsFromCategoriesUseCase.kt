package com.eva.recorderapp.voice_recorder.domain.use_cases

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.categories.models.RecordingCategoryModel
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ResourcedVoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RecordingsFromCategoriesUseCase(
	private val recordings: VoiceRecordingsProvider,
	private val secondaryDataProvider: RecordingsSecondaryDataProvider,
) {
	operator fun invoke(category: RecordingCategoryModel): Flow<ResourcedVoiceRecordingModels> {

		val extraMetaDataFlow = when (category) {
			RecordingCategoryModel.ALL_CATEGORY -> secondaryDataProvider.providesRecordingMetaData
			else -> secondaryDataProvider.recordingsFromCategory(category)
		}

		return combine(
			recordings.voiceRecordingsFlow,
			extraMetaDataFlow
		) { originalMetaData, extraMetaData ->
			try {
				val selectedData = when (category) {
					RecordingCategoryModel.ALL_CATEGORY -> originalMetaData
					else -> {
						val matchingIds = extraMetaData.map { it.recordingId }
						originalMetaData.filter { it.id in matchingIds }
					}
				}
				val result = CombineRecordingSources.combineMetadata(selectedData, extraMetaData)
				Resource.Success(result)
			} catch (e: Exception) {
				e.printStackTrace()
				Resource.Error(e)
			}
		}
	}
}
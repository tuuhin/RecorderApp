package com.eva.use_case.usecases

import com.eva.categories.domain.models.RecordingCategoryModel
import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.recordings.domain.provider.ResourcedVoiceRecordingModels
import com.eva.recordings.domain.provider.VoiceRecordingsProvider
import com.eva.utils.Resource
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
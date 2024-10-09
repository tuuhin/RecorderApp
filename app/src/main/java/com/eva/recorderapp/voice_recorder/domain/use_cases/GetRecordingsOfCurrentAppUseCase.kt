package com.eva.recorderapp.voice_recorder.domain.use_cases

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ExtraRecordingMetaDataList
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ResourcedVoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.util.AppWidgetsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

class GetRecordingsOfCurrentAppUseCase(
	private val recordings: VoiceRecordingsProvider,
	private val secondaryRecordings: RecordingsSecondaryDataProvider,
	private val widgetRepository: AppWidgetsRepository,
) {
	operator fun invoke(): Flow<ResourcedVoiceRecordingModels> {
		return combine(
			recordings.voiceRecordingsOnlyThisApp,
			secondaryRecordings.providesRecordingMetaData
		) { resource, metadata ->
			when (resource) {
				is Resource.Success -> {
					// emit the merged data
					val combinedData = combineMetadata(resource.data, metadata)
					Resource.Success(combinedData)
				}
				// on other cases emit that res only
				else -> resource
			}
		}.onEach {
			// update the widget on each emit
			widgetRepository.recordingsWidgetUpdate()
		}
	}

	private fun combineMetadata(
		recordings: VoiceRecordingModels,
		otherMetadata: ExtraRecordingMetaDataList,
	): VoiceRecordingModels {

		val recordingsIdWithExtraMetadata = otherMetadata.associateBy { it.recordingId }
		val recordingsKeys = recordingsIdWithExtraMetadata.keys

		return recordings.map { model ->
			// if not found return model
			if (!recordingsKeys.contains(model.id)) return@map model
			// if found add the extra data
			recordingsIdWithExtraMetadata.getOrDefault(model.id, null)
				?.let { extraData ->
					model.copy(
						isFavorite = extraData.isFavourite,
						categoryId = extraData.categoryId
					)
				} ?: model
		}.sortedBy { it.recordedAt }
	}
}
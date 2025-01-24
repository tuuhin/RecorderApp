package com.eva.recorderapp.voice_recorder.domain.use_cases

import com.eva.recorderapp.common.Resource
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.RecordingsSecondaryDataProvider
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.ResourcedVoiceRecordingModels
import com.eva.recorderapp.voice_recorder.domain.recordings.provider.VoiceRecordingsProvider
import com.eva.recorderapp.voice_recorder.domain.interactions.AppWidgetsRepository
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
					val combinedData = CombineRecordingSources.combineMetadata(resource.data, metadata)
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


}
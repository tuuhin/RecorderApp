package com.eva.use_case.usecases

import com.eva.recordings.domain.provider.RecordingsSecondaryDataProvider
import com.eva.recordings.domain.provider.ResourcedVoiceRecordingModels
import com.eva.recordings.domain.provider.VoiceRecordingsProvider
import com.eva.recordings.domain.widgets.RecordingWidgetInteractor
import com.eva.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

class GetRecordingsOfCurrentAppUseCase(
	private val recordings: VoiceRecordingsProvider,
	private val secondaryRecordings: RecordingsSecondaryDataProvider,
	private val widgetInteractor: RecordingWidgetInteractor,
) {
	operator fun invoke(): Flow<ResourcedVoiceRecordingModels> {
		return combine(
			recordings.voiceRecordingsOnlyThisApp,
			secondaryRecordings.providesRecordingMetaData
		) { resource, metadata ->
			when (resource) {
				is Resource.Success -> {
					// emit the merged data
					val combinedData =
						CombineRecordingSources.combineMetadata(resource.data, metadata)
					Resource.Success(combinedData)
				}
				// on other cases emit that res only
				else -> resource
			}
		}.onEach { widgetInteractor.invoke() }
	}
}
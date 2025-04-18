package com.eva.use_case.usecases

import com.eva.recordings.domain.models.ExtraRecordingMetadataModel
import com.eva.recordings.domain.provider.ExtraRecordingMetaDataList
import com.eva.recordings.domain.provider.VoiceRecordingModels
import kotlinx.collections.immutable.toPersistentHashMap

object CombineRecordingSources {

	fun combineMetadata(
		actualRecordings: VoiceRecordingModels,
		savedMetadata: ExtraRecordingMetaDataList,
	): VoiceRecordingModels {

		val recordingsIdMap = savedMetadata.associateBy(ExtraRecordingMetadataModel::recordingId)
			.toPersistentHashMap()

		return actualRecordings.map { model ->
			// if model id  is not found return the model
			val extraData = recordingsIdMap.getOrDefault(model.id, null) ?: return@map model
			// otherwise update the parameters
			model.copy(
				isFavorite = extraData.isFavourite,
				categoryId = extraData.categoryId
			)
		}
	}
}
package com.eva.recorderapp.voice_recorder.presentation.recordings.util.event

import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel

sealed interface DeleteOrTrashRecordingsRequest {

	data class OnTrashRequest(
		val recordings: Collection<RecordedVoiceModel>
	) : DeleteOrTrashRecordingsRequest

	data class OnDeleteRequest(
		val trashRecordings: Collection<TrashRecordingModel>
	) : DeleteOrTrashRecordingsRequest
}
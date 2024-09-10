package com.eva.recorderapp.voice_recorder.presentation.recordings.util.event

import androidx.activity.result.IntentSenderRequest
import com.eva.recorderapp.voice_recorder.domain.recordings.models.RecordedVoiceModel
import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel

sealed interface DeleteOrTrashRecordingsRequest {

	data class OnTrashRequest(
		val recordings: Collection<RecordedVoiceModel>,
		val intentSenderRequest: IntentSenderRequest? = null,
	) : DeleteOrTrashRecordingsRequest

	data class OnDeleteRequest(
		val trashRecordings: Collection<TrashRecordingModel>,
		val intentSenderRequest: IntentSenderRequest? = null,
	) : DeleteOrTrashRecordingsRequest
}
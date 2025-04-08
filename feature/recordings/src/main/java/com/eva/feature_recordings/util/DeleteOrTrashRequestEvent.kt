package com.eva.feature_recordings.util

import androidx.activity.result.IntentSenderRequest
import com.eva.recordings.domain.models.RecordedVoiceModel
import com.eva.recordings.domain.models.TrashRecordingModel

sealed interface DeleteOrTrashRequestEvent {

	data class OnTrashRequest(
		val recordings: Collection<RecordedVoiceModel>,
		val intentSenderRequest: IntentSenderRequest? = null,
	) : DeleteOrTrashRequestEvent

	data class OnDeleteRequest(
		val trashRecordings: Collection<TrashRecordingModel>,
		val intentSenderRequest: IntentSenderRequest? = null,
	) : DeleteOrTrashRequestEvent
}
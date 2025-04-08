package com.eva.feature_recordings.bin.state

import com.eva.recordings.domain.models.TrashRecordingModel

interface TrashRecordingScreenEvent {

	data object PopulateTrashRecordings : TrashRecordingScreenEvent

	data class OnRecordingSelectOrUnSelect(
		val recording: TrashRecordingModel
	) : TrashRecordingScreenEvent

	data object OnSelectTrashRecording : TrashRecordingScreenEvent

	data object OnUnSelectTrashRecording : TrashRecordingScreenEvent

	data object OnSelectItemRestore : TrashRecordingScreenEvent

	data object OnSelectItemDeleteForeEver : TrashRecordingScreenEvent

	data class OnPostDeleteRequest(val message: String = "") : TrashRecordingScreenEvent
}
package com.eva.feature_recordings.bin.state

import com.eva.recordings.domain.models.TrashRecordingModel

internal data class SelectableTrashRecordings(
	val trashRecording: TrashRecordingModel,
	val isSelected: Boolean = false,
)

internal fun List<TrashRecordingModel>.toSelectableRecordings(): List<SelectableTrashRecordings> =
	map(::SelectableTrashRecordings)
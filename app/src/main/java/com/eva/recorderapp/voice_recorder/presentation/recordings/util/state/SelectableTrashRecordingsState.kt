package com.eva.recorderapp.voice_recorder.presentation.recordings.util.state

import com.eva.recorderapp.voice_recorder.domain.recordings.models.TrashRecordingModel

data class SelectableTrashRecordings(
	val trashRecording: TrashRecordingModel,
	val isSelected: Boolean = false,
)

fun List<TrashRecordingModel>.toSelectableRecordings(): List<SelectableTrashRecordings> =
	map(::SelectableTrashRecordings)
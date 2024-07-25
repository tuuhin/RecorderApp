package com.eva.recorderapp.voice_recorder.presentation.recordings.util.state

import com.eva.recorderapp.voice_recorder.domain.models.RecordedVoiceModel

data class SelectableRecordings(
	val recoding: RecordedVoiceModel,
	val isSelected: Boolean = false,
)

fun List<RecordedVoiceModel>.toSelectableRecordings(): List<SelectableRecordings> =
	map(::SelectableRecordings)
package com.eva.feature_recordings.recordings.state

import com.eva.recordings.domain.models.RecordedVoiceModel

internal data class SelectableRecordings(
	val recording: RecordedVoiceModel,
	val isSelected: Boolean = false,
)

internal fun List<RecordedVoiceModel>.toSelectableRecordings(): List<SelectableRecordings> =
	map(::SelectableRecordings)
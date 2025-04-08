package com.eva.feature_recordings.recordings.state

internal data class RecordingsSortInfo(
	val options: SortOptions = SortOptions.NAME,
	val order: SortOrder = SortOrder.ASC
)
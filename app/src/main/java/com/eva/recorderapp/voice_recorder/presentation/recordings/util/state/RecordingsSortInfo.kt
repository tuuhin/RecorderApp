package com.eva.recorderapp.voice_recorder.presentation.recordings.util.state

data class RecordingsSortInfo(
	val options: SortOptions = SortOptions.NAME,
	val order: SortOrder = SortOrder.ASC
)
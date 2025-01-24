package com.eva.recorderapp.voice_recorder.presentation.util

object SharedElementTransitionKeys {

	const  val RECORDINGS_LIST_SHARED_BOUNDS = "recordings_list_screen"
	const val RECORDING_BIN_SHARED_BOUNDS = "recording_bin_screen"

	fun categoryCardSharedBoundsTransition(id: Long = -1) = "category_card_container_transition_$id"
	fun recordSharedEntryTitle(id: Long) = "record_entry_${id}_title"
	fun recordSharedEntryContainer(id: Long) = "record_entry_${id}_container"
}
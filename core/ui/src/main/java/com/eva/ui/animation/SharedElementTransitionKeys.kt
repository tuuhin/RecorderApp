package com.eva.ui.animation

object SharedElementTransitionKeys {

	const val RECORDINGS_LIST_SHARED_BOUNDS = "recordings_list_screen"
	const val RECORDING_BIN_SHARED_BOUNDS = "recording_bin_screen"
	const val RECORDING_EDITOR_SHARED_BOUNDS = "recordings_editor_screen"

	fun categoryCardSharedBoundsTransition(id: Long = -1) = "category_card_container_transition_$id"
	fun recordSharedEntryTitle(id: Long) = "record_entry_${id}_title"
	fun recordSharedEntryContainer(id: Long) = "record_entry_${id}_container"
}
package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class PlayerGraphInfo(
	val waves: ImmutableList<Float> = persistentListOf(),
	val isLoaded: Boolean = false,
)

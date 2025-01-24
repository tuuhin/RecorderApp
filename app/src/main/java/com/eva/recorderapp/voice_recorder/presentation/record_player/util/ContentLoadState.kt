package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import androidx.compose.runtime.Composable
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel

sealed interface ContentLoadState {

	data object Loading : ContentLoadState
	data class Content(val data: AudioFileModel) : ContentLoadState
	data object Unknown : ContentLoadState

	@Composable
	fun OnContentOrOther(
		content: @Composable (AudioFileModel) -> Unit,
		onOther: @Composable () -> Unit,
	) {
		when (this) {
			is Content -> content(data)
			else -> onOther()
		}
	}

	@Composable
	fun OnContent(content: @Composable (AudioFileModel) -> Unit) {
		if (this is Content) content(data)
	}
}
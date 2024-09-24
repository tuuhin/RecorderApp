package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel

sealed interface AudioFileEvent {
	data object ShareCurrentAudioFile : AudioFileEvent
	data class ToggleIsFavourite(val file: AudioFileModel) : AudioFileEvent
}
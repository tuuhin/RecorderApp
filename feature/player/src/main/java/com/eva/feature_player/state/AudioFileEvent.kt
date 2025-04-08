package com.eva.feature_player.state

import com.eva.recordings.domain.models.AudioFileModel

internal sealed interface AudioFileEvent {
	data object ShareCurrentAudioFile : AudioFileEvent
	data class ToggleIsFavourite(val file: AudioFileModel) : AudioFileEvent
}
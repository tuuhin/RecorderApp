package com.eva.player_shared

import com.eva.recordings.domain.models.AudioFileModel

sealed interface UserAudioAction {
	data object ShareCurrentAudioFile : UserAudioAction
	data class ToggleIsFavourite(val file: AudioFileModel) : UserAudioAction
}
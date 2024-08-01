package com.eva.recorderapp.voice_recorder.presentation.record_player.util

sealed interface PlayerEvents {
	data object ShareCurrentAudioFile : PlayerEvents
}
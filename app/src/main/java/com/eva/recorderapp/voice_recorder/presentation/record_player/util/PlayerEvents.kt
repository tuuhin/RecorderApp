package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import com.eva.recorderapp.voice_recorder.domain.player.PlayerPlayBackSpeed
import com.eva.recorderapp.voice_recorder.domain.player.model.AudioFileModel
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface PlayerEvents {
	data object ShareCurrentAudioFile : PlayerEvents
	data object OnStartPlayer : PlayerEvents
	data object OnPausePlayer : PlayerEvents
	data object OnMutePlayer : PlayerEvents
	data class ToggleIsFavourite(val file: AudioFileModel) : PlayerEvents

	data class OnForwardByNDuration(
		val duration: Duration = 1.seconds,
	) : PlayerEvents

	data class OnRewindByNDuration(
		val duration: Duration = 1.seconds,
	) : PlayerEvents

	data class OnPlayerSpeedChange(
		val speed: PlayerPlayBackSpeed = PlayerPlayBackSpeed.NORMAL,
	) : PlayerEvents

	data class OnRepeatModeChange(val canRepeat: Boolean) : PlayerEvents
	data class OnSeekPlayer(val amount: Duration) : PlayerEvents
	data object OnSeekComplete : PlayerEvents
}
package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import com.eva.recorderapp.voice_recorder.domain.player.PlayerPlayBackSpeed
import kotlin.time.Duration

sealed interface PlayerEvents {
	data object ShareCurrentAudioFile : PlayerEvents
	data object OnStartPlayer : PlayerEvents
	data object OnPausePlayer : PlayerEvents
	data object OnMutePlayer : PlayerEvents
	data class OnForwardByNDuration(val duration: Duration) : PlayerEvents
	data class OnRewindByNDuration(val duration: Duration) : PlayerEvents
	data class OnPlayerSpeedChange(val speed: PlayerPlayBackSpeed) : PlayerEvents
	data class OnRepeatModeChange(val canRepeat: Boolean) : PlayerEvents
}
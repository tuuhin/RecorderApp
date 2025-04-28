package com.eva.feature_player.state

import com.eva.player.domain.model.PlayerPlayBackSpeed
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal sealed interface PlayerEvents {
	data object OnStartPlayer : PlayerEvents
	data object OnPausePlayer : PlayerEvents
	data object OnMutePlayer : PlayerEvents

	data class OnForwardByNDuration(val duration: Duration = 1.seconds) : PlayerEvents

	data class OnRewindByNDuration(val duration: Duration = 1.seconds) : PlayerEvents

	data class OnPlayerSpeedChange(val speed: PlayerPlayBackSpeed) : PlayerEvents

	data class OnRepeatModeChange(val canRepeat: Boolean) : PlayerEvents
	data class OnSeekPlayer(val amount: Duration) : PlayerEvents
}
package com.eva.player.domain.model

/**
 * Represents a snapshot of the current player configuration
 * @property playerState The current operational state of the player. Defaults to [PlayerState.IDLE]
 * @property playBackSpeed The current playback speed of the media. Defaults to [PlayerPlayBackSpeed.Normal].
 * @property isRepeating A flag indicating whether the media will loop after it finishes.
 * @property isMuted A flag indicating whether the player's audio is muted.
 *
 * @see PlayerState
 * @see PlayerPlayBackSpeed
 */
data class PlayerMetaData(
	val playerState: PlayerState = PlayerState.IDLE,
	val playBackSpeed: PlayerPlayBackSpeed = PlayerPlayBackSpeed.Normal,
	val isRepeating: Boolean = false,
	val isMuted: Boolean = false,
)
package com.eva.feature_player.state

import com.eva.player.domain.model.PlayerMetaData
import com.eva.player.domain.model.PlayerTrackData

internal data class AudioPlayerState(
	val trackData: PlayerTrackData = PlayerTrackData(),
	val playerMetaData: PlayerMetaData = PlayerMetaData(),
	val isControllerSet: Boolean = false,
)
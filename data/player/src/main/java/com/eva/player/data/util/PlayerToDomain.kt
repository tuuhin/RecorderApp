package com.eva.player.data.util

import androidx.media3.common.Player
import com.eva.player.domain.model.PlayerTrackData
import kotlin.time.Duration.Companion.milliseconds

val Player.toTrackData: PlayerTrackData
	get() {
		val current = currentPosition.milliseconds
		val total = duration.milliseconds
		return PlayerTrackData(current = current, total = total)
	}
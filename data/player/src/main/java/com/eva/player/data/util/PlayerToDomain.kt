package com.eva.player.data.util

import androidx.media3.common.Player
import com.eva.player.domain.model.PlayerTrackData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

suspend fun Player.toTrackData(): PlayerTrackData {
	// player methods can be called only in main scope
	return withContext(Dispatchers.Main) {
		val current = currentPosition.milliseconds
		val total = duration.milliseconds
		PlayerTrackData(current = current, total = total)
	}
}
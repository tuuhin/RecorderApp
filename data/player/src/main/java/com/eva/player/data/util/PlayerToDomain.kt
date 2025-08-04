package com.eva.player.data.util

import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Player
import com.eva.player.domain.model.PlayerTrackData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

suspend fun Player.toTrackData(): PlayerTrackData {
	return if (Looper.myLooper() == Looper.getMainLooper()) readTrackData()
	else withContext(Dispatchers.Main) { readTrackData() }
}

private fun Player.readTrackData(): PlayerTrackData {
	val currentPos = currentPosition.takeIf { it != C.TIME_UNSET }?.milliseconds ?: Duration.ZERO
	val durationMs = duration.takeIf { it != C.TIME_UNSET }?.milliseconds ?: Duration.ZERO
	return PlayerTrackData(current = currentPos, total = durationMs)
}
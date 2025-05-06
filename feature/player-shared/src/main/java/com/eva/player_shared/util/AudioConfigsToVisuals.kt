package com.eva.player_shared.util

import android.util.Log
import com.eva.editor.domain.model.AudioEditAction
import com.eva.player_shared.AudioConfigToActionList
import com.eva.utils.RecorderConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

private const val TAG = "PLAYER_CONFIG_SETTER"

internal suspend fun FloatArray.updateArrayViaConfigs(configs: AudioConfigToActionList)
		: FloatArray {
	return withContext(Dispatchers.Default) {
		if (configs.isEmpty()) return@withContext this@updateArrayViaConfigs
		var modifiedArray = copyOf()

		Log.d(TAG, "INITIAL SIZE :${size}")

		val timeInMillisPerBar = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE

		// need to apply the config from back to front
		configs.reversed().forEachIndexed { iter, (config, action) ->
			val startIdx = (config.start.inWholeMilliseconds / timeInMillisPerBar).toInt()
			val endIdx = (config.end.inWholeMilliseconds / timeInMillisPerBar).toInt()

			val loopStart = max(0, startIdx)
			val loopEnd = min(size, endIdx + 1)

			val message = when (action) {
				AudioEditAction.CROP -> "NEW START :${loopStart} NEW END:$loopEnd"
				AudioEditAction.CUT -> "START1 :0 END1:$loopStart || START2 :$loopEnd END2: $size"
			}

			Log.i(TAG, "ITERATION:$iter $message")

			modifiedArray = when (action) {
				AudioEditAction.CUT -> modifiedArray.copyOfRange(0, loopStart + 1) +
						modifiedArray.copyOfRange(loopEnd - 1, size)

				AudioEditAction.CROP ->
					modifiedArray.copyOfRange(loopStart, loopEnd)
			}
		}
		val array = modifiedArray.filterNot { it == -1f }
		Log.d(TAG, "FINAL ARRAY SIZE AFTER UPDATE${array.size}")
		array.toFloatArray()
	}
}
package com.eva.player_shared.util

import android.util.Log
import com.eva.editor.domain.AudioConfigToActionList
import com.eva.editor.domain.model.AudioEditAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

private const val TAG = "PLAYER_CONFIG_SETTER"

internal suspend fun FloatArray.updateArrayViaConfigs(
	configs: AudioConfigToActionList,
	timeInMillisPerBar: Int = 100
): FloatArray {
	return withContext(Dispatchers.Default) {
		if (configs.isEmpty()) return@withContext this@updateArrayViaConfigs
		var modifiedArray = copyOf()

		Log.d(TAG, "INITIAL SIZE :${size}")

		// need to apply the config from back to front
		configs.reversed().forEachIndexed { iteration, (config, action) ->
			val startSample = (config.start.inWholeMilliseconds / timeInMillisPerBar).toInt()
			val endSample = (config.end.inWholeMilliseconds / timeInMillisPerBar).toInt()

			val validStart = max(0, min(startSample, modifiedArray.size))
			val validEnd = max(0, min(endSample, modifiedArray.size))

			if (validStart <= validEnd) {

				val message = when (action) {
					AudioEditAction.CROP -> "NEW START :${validStart} NEW END:$validEnd"
					AudioEditAction.CUT -> "START1 :0 END1:$validStart || START2 :$validEnd END2: ${modifiedArray.size}"
				}

				Log.i(TAG, "ITERATION:$iteration $message")

				modifiedArray = when (action) {
					AudioEditAction.CUT -> {
						val before = modifiedArray.copyOfRange(0, validStart)
						val after = modifiedArray.copyOfRange(validEnd, modifiedArray.size)
						before + after
					}

					AudioEditAction.CROP ->
						modifiedArray.copyOfRange(validStart, validEnd)
				}
			} else {
				val error = "Invalid clip: $validStart, $validEnd. $action at index $iteration"
				Log.w(TAG, error)
			}
		}
		Log.d(TAG, "Final array size after processing: ${modifiedArray.size}")
		modifiedArray
	}
}
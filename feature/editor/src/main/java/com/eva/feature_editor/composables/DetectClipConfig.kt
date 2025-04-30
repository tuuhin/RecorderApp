package com.eva.feature_editor.composables

import android.util.Log
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.eva.editor.data.AudioClipConfig
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun Modifier.detectClipConfig(
	onClipChange: (AudioClipConfig) -> Unit,
	totalLength: Duration,
	clipConfig: AudioClipConfig? = null,
	minClipAmount: Duration = 1.seconds,
	onMinClipAmountCrossed: () -> Unit = {},
) = composed {

	var localClipConfig by remember(totalLength) {
		val supposeToBe = AudioClipConfig(0.milliseconds, totalLength)
		mutableStateOf(clipConfig ?: supposeToBe)
	}

	val currentOnClipChange by rememberUpdatedState(onClipChange)
	val currentMinClipAmountCrossed by rememberUpdatedState(onMinClipAmountCrossed)

	pointerInput(totalLength) {
		var startX = 0f
		var endX = 0f
		var isStartDragging = false
		var isEndDragging = false
		val width = size.width

		detectHorizontalDragGestures(
			onDragStart = { offset ->
				with(localClipConfig) {
					val startPOs = (start / totalLength).toFloat() * width
					val endPos = (end / totalLength).toFloat() * width
					// set is dragging
					isStartDragging = abs(offset.x - startPOs) <= 50.dp.toPx()
					isEndDragging = abs(offset.x - endPos) <= 50.dp.toPx()
				}
				// set start offset
				startX = offset.x
				endX = offset.x
				Log.d("TAG", "DRAG STAR:$isStartDragging DRAG END:$isEndDragging")
			},
			onHorizontalDrag = { change, amount ->
				val timeDelta = (totalLength.inWholeMilliseconds * (amount / width)).toLong()
				if (isStartDragging) {
					val dragNewDuration = localClipConfig.start + timeDelta.milliseconds

					val finalNewDuration = if (dragNewDuration <= Duration.ZERO) Duration.ZERO
					else if (localClipConfig.end - dragNewDuration <= minClipAmount) {
						currentMinClipAmountCrossed()
						localClipConfig.start
					} else dragNewDuration

					localClipConfig = localClipConfig.copy(start = finalNewDuration)
					startX += amount
					currentOnClipChange(localClipConfig)
				} else if (isEndDragging) {
					val dragNewDuration = localClipConfig.end + timeDelta.milliseconds

					val finalNewDuration = if (dragNewDuration >= totalLength) totalLength
					else if (dragNewDuration - localClipConfig.start <= minClipAmount) {
						onMinClipAmountCrossed()
						localClipConfig.end
					} else dragNewDuration

					localClipConfig = localClipConfig.copy(end = finalNewDuration)
					endX += amount
					currentOnClipChange(localClipConfig)
				}
				// consume the changes
				change.consume()
			},
			onDragEnd = {
				isStartDragging = false
				isEndDragging = false
			},
			onDragCancel = {
				isStartDragging = false
				isEndDragging = false
			}
		)
	}
}

package com.eva.feature_editor.composables

import android.util.Log
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.dp
import com.eva.editor.domain.model.AudioClipConfig
import com.eva.player_shared.util.PlayerGraphData
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val TAG = "CLIP CONFIG"

@OptIn(FlowPreview::class)
internal fun Modifier.detectClipConfig(
	graph: PlayerGraphData,
	onClipChange: (AudioClipConfig) -> Unit,
	totalLength: Duration,
	maxGraphPoints: Int = 100,
	clipConfig: AudioClipConfig? = null,
	minClipAmount: Duration = 1.seconds,
	enabled: Boolean = true,
	onMinClipAmountCrossed: () -> Unit = {},
) = composed(
	fullyQualifiedName = "com.eva.feature_editor.composables.detectClipConfig",
	keys = arrayOf(totalLength),
	inspectorInfo = debugInspectorInfo {
		name = "editor_detect_clip_config"
		properties["clip_config"] = clipConfig
		properties["track_duration"] = totalLength
		properties["min_clip_amount"] = minClipAmount
		properties["enabled"] = enabled
	},
) {

	// total length if lesser than min clip amount then  no pointer input allowed
	if (totalLength <= minClipAmount || !enabled) return@composed Modifier

	val mutex = remember { MutatorMutex() }
	val scope = rememberCoroutineScope()

	var localClipConfig by remember(totalLength) {
		val supposeToBe = AudioClipConfig(0.milliseconds, totalLength)
		mutableStateOf(clipConfig ?: supposeToBe)
	}

	val currentOnClipChange by rememberUpdatedState(onClipChange)
	val currentMinClipAmountCrossed by rememberUpdatedState(onMinClipAmountCrossed)

	LaunchedEffect(localClipConfig) {
		snapshotFlow { localClipConfig }.filterNot { config ->
			val start = config.start
			val end = config.end
			end - start <= minClipAmount
		}
			.debounce(100.milliseconds)
			.collectLatest { currentMinClipAmountCrossed() }
	}

	pointerInput(totalLength) {

		var startX = 0f
		var endX = 0f
		var isStartDragging = false
		var isEndDragging = false

		val eachPointSize = size.width / maxGraphPoints
		val samples = graph()
		val isCompressedGraph = samples.size >= maxGraphPoints

		val width = if (isCompressedGraph) size.width
		else samples.size * eachPointSize

		detectHorizontalDragGestures(
			onDragStart = { offset ->
				scope.launch {
					mutex.mutate(MutatePriority.Default) {
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
					}
				}
			},
			onHorizontalDrag = { change, amount ->
				scope.launch {
					mutex.mutate(priority = MutatePriority.UserInput) {
						val timeDelta =
							(totalLength.inWholeMilliseconds * (amount / width)).toLong()
						if (isStartDragging) {
							val dragNewDuration = localClipConfig.start + timeDelta.milliseconds

							val startDifference = localClipConfig.end - dragNewDuration
							val finalNewDuration =
								if (dragNewDuration <= Duration.ZERO) Duration.ZERO
								else if (startDifference <= minClipAmount) {
									Log.d(
										TAG,
										"CLIP FOUND $startDifference SHOULD BE $minClipAmount"
									)
									localClipConfig.start
								} else dragNewDuration

							localClipConfig = localClipConfig.copy(start = finalNewDuration)
							startX += amount
							currentOnClipChange(localClipConfig)
						} else if (isEndDragging) {
							val dragNewDuration = localClipConfig.end + timeDelta.milliseconds
							val endDifference = dragNewDuration - localClipConfig.start
							val finalNewDuration = if (dragNewDuration >= totalLength) totalLength
							else if (endDifference <= minClipAmount) {
								Log.d(TAG, "CLIP FOUND $endDifference SHOULD BE $minClipAmount")
								localClipConfig.end
							} else dragNewDuration

							localClipConfig = localClipConfig.copy(end = finalNewDuration)
							endX += amount

							currentOnClipChange(localClipConfig)
						}
						// consume the changes
						if (isStartDragging || isEndDragging)
							change.consume()
					}
				}
			},
			onDragEnd = {
				scope.launch {
					mutex.mutate(MutatePriority.PreventUserInput) {
						isStartDragging = false
						isEndDragging = false
					}
				}
			},
			onDragCancel = {
				scope.launch {
					mutex.mutate(MutatePriority.PreventUserInput) {
						isStartDragging = false
						isEndDragging = false
					}
				}
			}
		)
	}
}

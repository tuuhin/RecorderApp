package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import android.util.Log
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerGraphInfo
import com.eva.recorderapp.voice_recorder.presentation.util.PlayerGraphData
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlin.time.Duration.Companion.minutes

@Composable
fun PlayerAmplitudeGraph(
	trackData: PlayerTrackData,
	graphData: PlayerGraphData,
	modifier: Modifier = Modifier,
	barColor: Color = MaterialTheme.colorScheme.secondary,
	trackPointerColor: Color = MaterialTheme.colorScheme.tertiary,
	backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	shape: Shape = MaterialTheme.shapes.medium
) {
// TODO: Make it performat to show larger dataset's

	var isDragStarted by remember { mutableStateOf(false) }

	Surface(
		color = backgroundColor,
		shape = shape,
		modifier = modifier
	) {
		Spacer(
			modifier = Modifier
				.padding(all = dimensionResource(id = R.dimen.graph_card_padding))
				.defaultMinSize(minHeight = dimensionResource(id = R.dimen.line_graph_min_height))
				.pointerInput(Unit) {
					detectHorizontalDragGestures(
						onDragStart = { isDragStarted = true },
						onDragEnd = { isDragStarted = false },
						onHorizontalDrag = { change, dragAmount ->
							change.consume()
							val amount = dragAmount.coerceIn(0f, size.width.toFloat())
							Log.d("DRAG CHANGES", "$amount")
						},
					)
				}
				.drawWithCache {

					val spikesWidth = 2.dp.toPx()
					val spikeSpace = 2.dp.toPx()
					val centerYAxis = size.height / 2
					val spikes = mutableListOf<Pair<Offset, Offset>>()
					val dots = mutableListOf<Offset>()

					val samples = graphData()

					samples.waves.forEachIndexed { idx, value ->
						val sizeFactor = 0.75f * value
						val xAxis = (spikesWidth + spikeSpace) * idx.toFloat()
						val start = Offset(xAxis, centerYAxis * (1 - sizeFactor))
						val end = Offset(xAxis, centerYAxis * (1 + sizeFactor))
						if (start.y != end.y) spikes.add(Pair(start, end))
						else dots.add(start)
					}

					val totalSize = spikes.lastOrNull()?.first?.x ?: size.width

					onDrawBehind {

						val translate = size.width / 2 - (totalSize * trackData.playRatio)

						translate(left = translate) {
							spikes.forEach { (start, end) ->
								drawLine(
									color = barColor,
									start = start,
									end = end,
									strokeWidth = spikesWidth,
									cap = StrokeCap.Round
								)
							}
							drawPoints(
								points = dots,
								pointMode = PointMode.Points,
								color = barColor,
								strokeWidth = spikeSpace,
								cap = StrokeCap.Round
							)
						}
						drawCircle(
							color = trackPointerColor,
							radius = spikesWidth + spikeSpace,
							center = Offset(size.width / 2, 0f)
						)
						drawCircle(
							color = trackPointerColor,
							radius = spikesWidth + spikeSpace,
							center = Offset(size.width / 2, size.height)
						)
						drawLine(
							color = trackPointerColor,
							start = Offset(size.width / 2, 0f),
							end = Offset(size.width / 2, size.height),
							strokeWidth = spikesWidth,
							cap = StrokeCap.Round
						)
					}
				},
		)
	}
}

@PreviewLightDark
@Composable
private fun PlayerAmplitudeGraphPreview() = RecorderAppTheme {
	PlayerAmplitudeGraph(
		trackData = PlayerTrackData(current = 5.minutes, total = 10.minutes),
		graphData = { PlayerGraphInfo(waves = PreviewFakes.PREVIEW_RECORDER_AMPLITUDES) },
		modifier = Modifier.fillMaxWidth()
	)
}

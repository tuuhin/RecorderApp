package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import android.util.Log
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.common.LocalTimeFormats
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.data.util.asLocalTime
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.presentation.util.PlayerGraphData
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlayerAmplitudeGraph(
	trackData: PlayerTrackData,
	graphData: PlayerGraphData,
	bookMarks: ImmutableList<LocalTime>,
	modifier: Modifier = Modifier,
	plotColor: Color = MaterialTheme.colorScheme.secondary,
	trackPointerColor: Color = MaterialTheme.colorScheme.primary,
	bookMarkColor: Color = MaterialTheme.colorScheme.tertiary,
	timelineColor: Color = MaterialTheme.colorScheme.outline,
	timelineColorVariant: Color = MaterialTheme.colorScheme.outlineVariant,
	timelineTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
	timelineTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	shape: Shape = MaterialTheme.shapes.small,
	contentPadding: PaddingValues = PaddingValues(
		horizontal = dimensionResource(id = R.dimen.graph_card_padding),
		vertical = dimensionResource(id = R.dimen.graph_card_padding_other)
	),
) {
	val textMeasurer = rememberTextMeasurer()
	val image = painterResource(R.drawable.ic_bookmark)

	var isDragStarted by remember { mutableStateOf(false) }

	Surface(
		shape = shape,
		color = containerColor,
		modifier = modifier.aspectRatio(1.6f),
	) {
		Spacer(
			modifier = Modifier
				.padding(paddingValues = contentPadding)
				.defaultMinSize(minHeight = dimensionResource(id = R.dimen.line_graph_min_height))
				.pointerInput(Unit) {
					detectHorizontalDragGestures(
						onDragStart = { isDragStarted = true },
						onDragEnd = { isDragStarted = false },
						onHorizontalDrag = { change, dragAmount ->
							change.consume()
							val amount = dragAmount.coerceIn(0f, size.width.toFloat())
							Log.d("AMOUNT", "$amount")
						},
					)
				}
				.drawWithCache {

					val centerYAxis = size.height / 2f

					val spikesWidth = size.width / VoiceRecorder.RECORDER_AMPLITUDES_BUFFER_SIZE
					val spikeSpace = (spikesWidth - 1.5.dp.toPx()).let { amt ->
						if (amt > 0f) amt else 2.dp.toPx()
					}

					val bookMarksAsMillis = bookMarks.map { it.toMillisecondOfDay() }


					onDrawBehind {
						val samples = graphData()

						val totalSize = samples.size * spikesWidth
						val translate = size.width * .5f - (totalSize * trackData.playRatio)

						translate(left = translate) {
							drawGraph(
								waves = samples,
								centerYAxis = centerYAxis,
								spikesGap = spikeSpace,
								spikesWidth = spikesWidth,
								color = plotColor
							)
							drawTimeLine(
								totalDuration = trackData.totalAsLocalTime,
								textMeasurer = textMeasurer,
								bookMarks = bookMarksAsMillis,
								bookMarkPainter = image,
								spikesWidth = spikesWidth,
								outlineColor = timelineColor,
								outlineVariant = timelineColorVariant,
								textStyle = timelineTextStyle,
								textColor = timelineTextColor,
								bookMarkColor = bookMarkColor
							)
						}
						drawTrackPointer(
							color = trackPointerColor,
							radius = spikesWidth,
							strokeWidth = spikesWidth
						)
					}
				},
		)
	}
}

private fun DrawScope.drawGraph(
	waves: List<Float>,
	centerYAxis: Float,
	spikesGap: Float = 2f,
	spikesWidth: Float = 2f,
	color: Color = Color.Gray,
) {
	val dots = mutableListOf<Offset>()
	waves.forEachIndexed { idx, value ->
		val sizeFactor = value * .8f
		val xAxis = spikesWidth * idx.toFloat()
		val start = Offset(xAxis, centerYAxis * (1 - sizeFactor))
		val end = Offset(xAxis, centerYAxis * (1 + sizeFactor))
		if (start.y != end.y) {
			drawLine(
				color = color,
				start = start,
				end = end,
				strokeWidth = spikesGap,
				cap = StrokeCap.Round
			)
		} else dots.add(start)
	}

	drawPoints(
		points = dots,
		pointMode = PointMode.Points,
		color = color,
		strokeWidth = spikesGap
	)


}

private fun DrawScope.drawTimeLine(
	totalDuration: LocalTime,
	bookMarks: List<Int>,
	bookMarkPainter: Painter,
	textMeasurer: TextMeasurer,
	outlineColor: Color = Color.Gray,
	outlineVariant: Color = Color.Gray,
	bookMarkColor: Color = Color.Cyan,
	spikesWidth: Float = 2f,
	strokeWidthThick: Float = 2f,
	strokeWidthLight: Float = 1f,
	textStyle: TextStyle = TextStyle(),
	textColor: Color = Color.Black,
) {
	// 2000 for extra 2 seconds on the graph
	val durationAsMillis = totalDuration.toMillisecondOfDay() + 2_000
	val spacing = spikesWidth / VoiceRecorder.RECORDER_AMPLITUDES_BUFFER_SIZE

	repeat(durationAsMillis) { millis ->
		if (millis % 2_000 == 0) {

			val xAxis = millis * spacing
			val time = millis.milliseconds.asLocalTime
			val readable = time.format(LocalTimeFormats.LOCALTIME_FORMAT_MM_SS)

			val layoutResult = textMeasurer.measure(readable, style = textStyle)
			val textOffset = with(layoutResult) {
				Offset(size.width / 2f, size.height / 2f)
			}

			drawText(
				textLayoutResult = layoutResult,
				topLeft = Offset(xAxis, -1 * 8.dp.toPx()) - textOffset,
				color = textColor,
			)

			drawLine(
				color = outlineColor,
				start = Offset(xAxis, 0f),
				end = Offset(xAxis, 8.dp.toPx()),
				strokeWidth = strokeWidthThick,
				cap = StrokeCap.Round,
			)
			drawLine(
				color = outlineColor,
				start = Offset(xAxis, size.height - 8.dp.toPx()),
				end = Offset(xAxis, size.height),
				strokeWidth = strokeWidthThick
			)
		} else if (millis % 500 == 0) {
			val xAxis = millis * spacing
			drawLine(
				color = outlineVariant,
				start = Offset(xAxis, 0f),
				end = Offset(xAxis, 4.dp.toPx()),
				strokeWidth = strokeWidthLight,
				cap = StrokeCap.Round,
			)
			drawLine(
				color = outlineVariant,
				start = Offset(xAxis, size.height - 4.dp.toPx()),
				end = Offset(xAxis, size.height),
				strokeWidth = strokeWidthLight,
				cap = StrokeCap.Round,
			)
		}

		if (millis in bookMarks) {
			val xAxis = millis * spacing
			drawLine(
				color = bookMarkColor,
				start = Offset(xAxis, 2.dp.toPx()),
				end = Offset(xAxis, size.height - 2.dp.toPx()),
				strokeWidth = strokeWidthThick,
				cap = StrokeCap.Round
			)

			drawCircle(
				color = bookMarkColor,
				radius = 3.dp.toPx(),
				center = Offset(xAxis, 2.dp.toPx())
			)

			val imageSize = 12.dp

			translate(
				left = xAxis - (imageSize.toPx() / 2f),
				top = size.height + 4.dp.toPx()
			) {
				with(bookMarkPainter) {
					draw(
						size = Size(imageSize.toPx(), imageSize.toPx()),
						colorFilter = ColorFilter.tint(bookMarkColor)
					)
				}
			}
		}
	}
}

private fun DrawScope.drawTrackPointer(
	color: Color = Color.Black,
	radius: Float = 1f,
	strokeWidth: Float = 1f,
) {
	drawCircle(
		color = color,
		radius = radius,
		center = Offset(size.width / 2, 0f)
	)
	drawCircle(
		color = color,
		radius = strokeWidth,
		center = Offset(size.width / 2, size.height)
	)
	drawLine(
		color = color,
		start = Offset(size.width / 2, 0f),
		end = Offset(size.width / 2, size.height),
		strokeWidth = strokeWidth,
		cap = StrokeCap.Round
	)
}

@PreviewLightDark
@Composable
private fun PlayerAmplitudeGraphPreview() = RecorderAppTheme {
	PlayerAmplitudeGraph(
		trackData = PlayerTrackData(current = 5.seconds, total = 10.seconds),
		graphData = { PreviewFakes.PREVIEW_RECORDER_AMPLITUDES },
		bookMarks = persistentListOf(LocalTime.fromSecondOfDay(2), LocalTime.fromSecondOfDay(8)),
		modifier = Modifier.fillMaxWidth()
	)
}

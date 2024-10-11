package com.eva.recorderapp.voice_recorder.presentation.record_player.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.common.LocalTimeFormats
import com.eva.recorderapp.common.asLocalTime
import com.eva.recorderapp.voice_recorder.domain.recorder.RecorderConstants
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlin.time.Duration.Companion.milliseconds

object PlayerGraphDrawUtil {

	fun DrawScope.drawGraph(
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
			points = dots, pointMode = PointMode.Points, color = color, strokeWidth = spikesGap
		)


	}

	fun DrawScope.drawTimeLine(
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
		val spacing = spikesWidth / RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE

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
					strokeWidth = strokeWidthThick,
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
					cap = StrokeCap.Round,
				)

				drawCircle(
					color = bookMarkColor,
					radius = 3.dp.toPx(),
					center = Offset(xAxis, 2.dp.toPx()),
				)

				val imageSize = 12.dp

				translate(
					left = xAxis - (imageSize.toPx() / 2f), top = size.height + 4.dp.toPx()
				) {
					with(bookMarkPainter) {
						draw(
							size = Size(imageSize.toPx(), imageSize.toPx()),
							colorFilter = ColorFilter.tint(bookMarkColor),
						)
					}
				}
			}
		}
	}

	fun DrawScope.drawTrackPointer(
		color: Color = Color.Black,
		radius: Float = 1f,
		strokeWidth: Float = 1f,
	) {
		drawCircle(
			color = color,
			radius = radius,
			center = Offset(size.width / 2, 0f),
		)
		drawCircle(
			color = color,
			radius = strokeWidth,
			center = Offset(size.width / 2, size.height),
		)
		drawLine(
			color = color,
			start = Offset(size.width / 2, 0f),
			end = Offset(size.width / 2, size.height),
			strokeWidth = strokeWidth,
			cap = StrokeCap.Round,
		)
	}
}
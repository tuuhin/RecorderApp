package com.eva.feature_recorder.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.eva.utils.LocalTimeFormats
import com.eva.utils.toLocalTime
import kotlinx.datetime.format
import kotlin.time.Duration

internal fun DrawScope.drawAmplitudeGraph(
	amplitudes: List<Float>,
	centerYAxis: Float,
	spikesGap: Float = 2f,
	spikesWidth: Float = 2f,
	barColor: Color = Color.Gray,
	debug: Boolean = false,
) {
	amplitudes.forEachIndexed { idx, value ->
		val scaleValue = value * .8f
		val xAxis = spikesWidth * idx.toFloat()
		val start = Offset(xAxis, centerYAxis * (1 - scaleValue))
		val end = Offset(xAxis, centerYAxis * (1 + scaleValue))
		if (start.y != end.y) {
			// draw graph-line
			drawLine(
				color = barColor,
				start = start,
				end = end,
				strokeWidth = spikesGap,
				cap = StrokeCap.Round
			)
		}
		// debug lines to show the limit range
		if (idx + 1 == amplitudes.size && debug) {
			drawLine(
				color = Color.Red,
				start = start, end = end,
				strokeWidth = 2.dp.toPx()
			)
		}
	}
}

internal fun DrawScope.drawRecorderTimeline(
	image: Painter,
	timeline: Collection<Duration>,
	bookMarks: Collection<Duration>,
	textMeasurer: TextMeasurer,
	outlineColor: Color = Color.Gray,
	outlineVariant: Color = Color.Gray,
	bookMarkColor: Color = Color.Blue,
	spikesWidth: Float = 2f,
	strokeWidthThick: Float = 2f,
	strokeWidthLight: Float = 1f,
	textStyle: TextStyle = TextStyle(),
	textColor: Color = Color.Black,
) {
	timeline.forEachIndexed { idx, duration ->
		val xAxis = spikesWidth * idx.toFloat()
		val timeInMillis = duration.inWholeMilliseconds

		if (timeInMillis.mod(2_000) == 0 || idx == 0) {

			val readableTime = duration.toLocalTime()
				.format(LocalTimeFormats.LOCALTIME_FORMAT_MM_SS)

			val layoutResult = textMeasurer.measure(readableTime, style = textStyle)
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
		} else if (timeInMillis.mod(500) == 0) {
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

		if (duration in bookMarks) {

			drawLine(
				color = bookMarkColor,
				start = Offset(xAxis, 2.dp.toPx()),
				end = Offset(xAxis, size.height - 2.dp.toPx()),
				strokeWidth = strokeWidthThick,
				cap = StrokeCap.Round
			)

			drawCircle(
				color = bookMarkColor, radius = 3.dp.toPx(),
				center = Offset(xAxis, 2.dp.toPx())
			)

			val imageSize = 12.dp

			translate(
				left = xAxis - (imageSize.toPx() / 2f), top = size.height + 4.dp.toPx()
			) {
				with(image) {
					draw(
						size = Size(imageSize.toPx(), imageSize.toPx()),
						colorFilter = ColorFilter.tint(bookMarkColor)
					)
				}
			}
		}
	}
}

package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
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
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.presentation.util.RecordingDataPointCallback
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format

@Composable
fun RecorderAmplitudeGraph(
	dataPointCallback: RecordingDataPointCallback,
	bookMarks: ImmutableList<LocalTime>,
	modifier: Modifier = Modifier,
	plotColor: Color = MaterialTheme.colorScheme.secondary,
	bookMarkColor: Color = MaterialTheme.colorScheme.tertiary,
	timelineColor: Color = MaterialTheme.colorScheme.outline,
	timelineColorVariant: Color = MaterialTheme.colorScheme.outlineVariant,
	timelineTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
	timelineTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	shape: Shape = MaterialTheme.shapes.small,
	contentPadding: PaddingValues = PaddingValues(
		horizontal = dimensionResource(id = R.dimen.graph_card_padding),
		vertical = dimensionResource(R.dimen.graph_card_padding_other)
	),
) {
	val textMeasurer = rememberTextMeasurer()
	val tag = painterResource(R.drawable.ic_bookmark)

	Surface(
		shape = shape,
		color = containerColor,
		modifier = modifier.aspectRatio(1.6f),
	) {
		Spacer(
			modifier = Modifier
				.padding(paddingValues = contentPadding)
				.defaultMinSize(minHeight = dimensionResource(id = R.dimen.line_graph_min_height))
				.drawWithCache {
					val blockSize = VoiceRecorder.RECORDER_AMPLITUDES_BUFFER_SIZE
					val centerYAxis = size.height / 2

					val spikesWidth = size.width / VoiceRecorder.RECORDER_AMPLITUDES_BUFFER_SIZE
					val spikesGap = (spikesWidth - 1.5.dp.toPx()).let { amt ->
						if (amt > 0f) amt else 2.dp.toPx()
					}

					onDrawBehind {
						val result = dataPointCallback()

						val amplitudes = result.map { it.second }

						val timeline = result.map { it.first }
						val paddedTimeline = timeline.padWithTime(blockSize)

						val translateLeft = if (result.size <= blockSize) 0f
						else (blockSize - result.size) * spikesWidth

						val bookmarksToDraw = if (timeline.isNotEmpty())
						// min is required not to draw extra lines and max ensures it doesn't
						// cross the line
							bookMarks.filter { time -> time > timeline.min() && time < timeline.max() }
						// otherwise its empty
						else emptyList()

						translate(left = translateLeft) {
							drawGraph(
								amplitudes = amplitudes,
								spikesGap = spikesGap,
								centerYAxis = centerYAxis,
								spikesWidth = spikesWidth,
								barColor = plotColor,
							)
							drawTimeLine(
								image = tag,
								timeline = paddedTimeline,
								bookMarks = bookmarksToDraw,
								textMeasurer = textMeasurer,
								spikesWidth = spikesWidth,
								strokeWidthThick = 2.dp.toPx(),
								strokeWidthLight = 1.25.dp.toPx(),
								bookMarkColor = bookMarkColor,
								outlineColor = timelineColor,
								outlineVariant = timelineColorVariant,
								textStyle = timelineTextStyle,
								textColor = timelineTextColor
							)
						}
					}
				},
		)
	}
}

private fun DrawScope.drawGraph(
	amplitudes: List<Float>,
	centerYAxis: Float,
	spikesGap: Float = 2f,
	spikesWidth: Float = 2f,
	barColor: Color = Color.Gray,
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
	}
}

private fun DrawScope.drawTimeLine(
	image: Painter,
	timeline: List<LocalTime>,
	bookMarks: List<LocalTime>,
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
	timeline.forEachIndexed { idx, time ->
		val xAxis = spikesWidth * idx.toFloat()

		if (idx.mod(20) == 0) {

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
		} else if (idx.mod(5) == 0) {
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

		if (time in bookMarks) {

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

private fun List<LocalTime>.padWithTime(blockSize: Int, extra: Int = 10): List<LocalTime> {
	val sizeDiff = blockSize - size
	val lastValue = lastOrNull() ?: LocalTime.fromMillisecondOfDay(0)
	// extra will create the translation effect properly
	val amount = if (sizeDiff >= 0) sizeDiff else 0
	return this + List(amount + extra) {
		val millis = lastValue.toMillisecondOfDay() + ((it + 1) * 100)
		LocalTime.fromMillisecondOfDay(millis)
	}
}

@PreviewLightDark
@Composable
private fun RecorderAmplitudeGraphPreview() = RecorderAppTheme {
	RecorderAmplitudeGraph(
		dataPointCallback = { PreviewFakes.PREVIEW_RECORDER_AMPLITUDES_FLOAT_ARRAY },
		bookMarks = persistentListOf(
			LocalTime.fromMillisecondOfDay(2_000),
		),
		modifier = Modifier.fillMaxWidth(),
	)
}

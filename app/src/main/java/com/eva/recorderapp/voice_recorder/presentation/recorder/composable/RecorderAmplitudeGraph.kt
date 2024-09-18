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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.dimensionResource
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
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toKotlinLocalTime

@Composable
fun RecorderAmplitudeGraph(
	dataPointCallback: RecordingDataPointCallback,
	bookMarks: ImmutableList<LocalTime>,
	modifier: Modifier = Modifier,
	barColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
	bookMarkColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
	axisColor: Color = MaterialTheme.colorScheme.outline,
	axisColorVariant: Color = MaterialTheme.colorScheme.outlineVariant,
	textStyle: TextStyle = MaterialTheme.typography.labelSmall,
	textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
	backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	shape: Shape = MaterialTheme.shapes.small,
	contentPadding: PaddingValues = PaddingValues(all = dimensionResource(id = R.dimen.graph_card_padding)),
) {
	val textMeasurer = rememberTextMeasurer()

	Surface(
		shape = shape,
		color = backgroundColor,
		modifier = modifier.aspectRatio(1.65f),
	) {
		Spacer(
			modifier = Modifier
				.padding(paddingValues = contentPadding)
				.defaultMinSize(minHeight = dimensionResource(id = R.dimen.line_graph_min_height))
				.drawWithCache {
					val blockSize = VoiceRecorder.RECORDER_AMPLITUDES_BUFFER_SIZE
					val centerYAxis = size.height / 2

					val spikesGap = 2.dp.toPx()
					val spikesWidth = size.width / VoiceRecorder.RECORDER_AMPLITUDES_BUFFER_SIZE

					onDrawBehind {
						val result = dataPointCallback()

						val amplitudes = result.map { it.second }
						val timeLine = result
							.map { it.first }
							.padWithTime(blockSize)

						val translateAmount = if (result.size <= blockSize) 0f
						else (blockSize - result.size) * spikesWidth

						translate(left = translateAmount) {
							drawGraph(
								amplitudes = amplitudes,
								spikesGap = spikesGap,
								centerYAxis = centerYAxis,
								spikesWidth = spikesWidth,
								barColor = barColor,
							)
							drawTimeLine(
								timeLine = timeLine,
								bookMarks = bookMarks,
								textMeasurer = textMeasurer,
								spikesWidth = spikesWidth,
								strokeWidthThick = spikesGap,
								strokeWidthLight = 1.5.dp.toPx(),
								bookMarkColor = bookMarkColor,
								outlineColor = axisColor,
								outlineVariant = axisColorVariant,
								textStyle = textStyle,
								textColor = textColor
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
	timeLine: List<LocalTime>,
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
	timeLine.forEachIndexed { idx, time ->
		val xAxis = spikesWidth * idx.toFloat()

		if (idx.mod(20) == 0) {

			val readable = time.format(LocalTimeFormats.LOCALTIME_FORMAT_MM_SS)
			val result = textMeasurer.measure(readable, style = textStyle, skipCache = true)
			val negativeOffset = Offset(x = result.size.width / 2f, y = result.size.height / 2f)

			drawText(
				textLayoutResult = result,
				topLeft = Offset(xAxis, -1 * 8.dp.toPx()) - negativeOffset,
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
				start = Offset(xAxis, 0f),
				end = Offset(xAxis, size.height),
				strokeWidth = strokeWidthThick,
				cap = StrokeCap.Round
			)
			drawCircle(
				color = bookMarkColor,
				radius = 3.dp.toPx(),
				center = Offset(xAxis, 0f)
			)
			drawCircle(
				color = bookMarkColor,
				radius = 3.dp.toPx(),
				center = Offset(xAxis, size.height)
			)
		}
	}
}

private fun List<LocalTime>.padWithTime(blockSize: Int, extra: Int = 10): List<LocalTime> {
	val sizeDiff = blockSize - size
	val lastValue = lastOrNull() ?: LocalTime.fromMillisecondOfDay(0)
	// extra will create the translation effect properly
	val amount = if (sizeDiff >= 0) sizeDiff else 0
	return this + List(amount + extra) {
		lastValue.toJavaLocalTime()
			.plusNanos((it + 1) * 100_000_000L)
			.toKotlinLocalTime()
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

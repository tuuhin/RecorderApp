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
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.presentation.util.RecordingAmplitudes

typealias Postitions = Pair<Offset, Offset>

@Composable
fun RecorderAmplitudeGraph(
	amplitudeCallback: RecordingAmplitudes,
	modifier: Modifier = Modifier,
	barColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
	backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
	shape: Shape = MaterialTheme.shapes.medium,
	contentPadding: PaddingValues = PaddingValues(12.dp)
) {

	Surface(
		shape = shape,
		color = backgroundColor,
		shadowElevation = 2.dp,
		tonalElevation = 2.dp,
		modifier = modifier,
	) {
		Spacer(
			modifier = Modifier
				.padding(contentPadding)
				.defaultMinSize(minHeight = dimensionResource(id = R.dimen.line_graph_min_height))
				.aspectRatio(1.78f)
				.drawWithCache {

					val spikesGap = 1.dp.toPx()
					val spikesWidth = size.width / VoiceRecorder.RECORDER_AMPLITUDES_BUFFER_SIZE

					val centerYAxis = size.height / 2
					val strokeWidth = spikesWidth - spikesGap

					onDrawBehind {

						val dots = mutableListOf<Offset>()

						val amplitudes = amplitudeCallback()

						amplitudes.forEachIndexed { idx, value ->
							val scaleValue = value * .85f
							val xAxis = (spikesWidth + spikesGap) * idx.toFloat()
							val start = Offset(xAxis, centerYAxis * (1 - scaleValue))
							val end = Offset(xAxis, centerYAxis * (1 + scaleValue))
							if (start.y != end.y) {
								// draw graph-line
								drawLine(
									color = barColor,
									start = start,
									end = end,
									strokeWidth = strokeWidth,
									cap = StrokeCap.Round
								)
							} else dots.add(start)
						}
						// minimum points where height is same
						drawPoints(
							points = dots,
							pointMode = PointMode.Points,
							color = barColor,
							strokeWidth = strokeWidth
						)
					}
				},
		)
	}
}

@PreviewLightDark
@Composable
private fun RecorderAmplitudeGraphPreview() = RecorderAppTheme {
	RecorderAmplitudeGraph(
		amplitudeCallback = { PreviewFakes.PREVIEW_RECORDER_AMPLITUDES },
		modifier = Modifier.fillMaxWidth()
	)
}


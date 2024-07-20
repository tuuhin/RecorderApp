package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList

@Composable
fun RecorderAmplitudeGraph(
	amplitudes: ImmutableList<Float>,
	modifier: Modifier = Modifier,
	barColor: Color = MaterialTheme.colorScheme.onTertiaryContainer,
	backgroundColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
	shape: Shape = MaterialTheme.shapes.medium
) {

	Surface(
		modifier = modifier,
		shape = shape,
		color = backgroundColor
	) {
		Spacer(
			modifier = Modifier
				.padding(all = dimensionResource(id = R.dimen.amplitudes_card_padding))
				.aspectRatio(2f)
				.drawWithCache {
					val spikesCount = amplitudes.size

					val spikesWidth = size.width / spikesCount
					val centerYAxis = size.height / 2

					val spikes = mutableListOf<Pair<Offset, Offset>>()
					val dots = mutableListOf<Offset>()

					for (idx in 0..<amplitudes.size) {
						val normal = amplitudes.getOrNull(idx) ?: 0f

						val xAxis = spikesWidth * idx.toFloat()
						val start = Offset(xAxis, centerYAxis * (1 - normal))
						val end = Offset(xAxis, centerYAxis * (1 + normal))
						if (start.y != end.y) spikes.add(Pair(start, end))
						else dots.add(start)
					}

					onDrawBehind {
						spikes.forEach { (start, end) ->
							drawLine(
								color = barColor,
								start = start,
								end = end,
								strokeWidth = spikesWidth - 2.dp.toPx(),
								cap = StrokeCap.Round
							)
						}
						drawPoints(
							points = dots,
							pointMode = PointMode.Points,
							color = barColor,
							strokeWidth = 2.dp.toPx()
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
		amplitudes = PreviewFakes.PREVIEW_RECORDER_AMPLITUDES,
		modifier = Modifier
			.fillMaxWidth()

	)
}


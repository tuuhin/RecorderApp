package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.eva.player_shared.util.PlayRatio
import com.eva.player_shared.util.PlayerGraphData
import com.eva.player_shared.util.PlayerPreviewFakes
import com.eva.player_shared.util.drawGraph
import com.eva.player_shared.util.drawGraphCompressed
import com.eva.player_shared.util.drawTimeLine
import com.eva.player_shared.util.drawTimeLineCompressed
import com.eva.player_shared.util.drawTrackPointer
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun EditorAmplitudeGraph(
	playRatio: PlayRatio,
	totalTrackDuration: Duration,
	graphData: PlayerGraphData,
	modifier: Modifier = Modifier,
	maxGraphPoints: Int = 100,
	plotColor: Color = MaterialTheme.colorScheme.secondary,
	trackPointerColor: Color = MaterialTheme.colorScheme.primary,
	timelineColor: Color = MaterialTheme.colorScheme.outline,
	timelineColorVariant: Color = MaterialTheme.colorScheme.outlineVariant,
	timelineTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
	timelineTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
	val textMeasurer = rememberTextMeasurer()

	Spacer(
		modifier = modifier
			.defaultMinSize(minHeight = dimensionResource(id = R.dimen.line_graph_min_height))
			.drawWithCache {

				val eachPointSize = size.width / maxGraphPoints
				val spikeWidth = (eachPointSize - 1.5.dp.toPx()).let { amt ->
					if (amt > 0f) amt else 2.dp.toPx()
				}
				onDrawBehind {

					val samples = graphData().let { graphList ->
						if (graphList.size >= maxGraphPoints) graphList.take(maxGraphPoints)
							.toFloatArray()
						else graphList
					}

					val isCompressedGraph = samples.size >= maxGraphPoints

					val translateX = if (isCompressedGraph) size.width
					else samples.size * eachPointSize

					val translate = translateX * playRatio()

					if (isCompressedGraph) {
						// graph
						drawGraphCompressed(
							waves = samples,
							color = plotColor,
							spikesWidth = spikeWidth,
							drawPoints = false
						)
						// timeline
						drawTimeLineCompressed(
							totalDuration = totalTrackDuration,
							sampleSize = maxGraphPoints,
							textMeasurer = textMeasurer,
							outlineColor = timelineColor,
							outlineVariant = timelineColorVariant,
							textStyle = timelineTextStyle,
							textColor = timelineTextColor,
						)
					} else {
						//graph
						drawGraph(
							waves = samples,
							spikesWidth = eachPointSize,
							spikesGap = spikeWidth,
							color = plotColor,
							drawPoints = false
						)
						//timeline
						drawTimeLine(
							totalDuration = maxOf(totalTrackDuration, 10.seconds),
							textMeasurer = textMeasurer,
							sampleSize = maxGraphPoints,
							outlineColor = timelineColor,
							outlineVariant = timelineColorVariant,
							textStyle = timelineTextStyle,
							textColor = timelineTextColor,
							spikesWidth = eachPointSize,
						)
					}

					// draw the pointer
					drawTrackPointer(
						xAxis = translate,
						color = trackPointerColor,
						radius = spikeWidth,
						strokeWidth = eachPointSize,
					)
				}
			},
	)
}


private class TrackDurationPreviewParams
	: CollectionPreviewParameterProvider<Duration>(listOf(4.seconds, 9.seconds))

@Preview
@Composable
private fun EditorAmplitudeGraphPreview(
	@PreviewParameter(TrackDurationPreviewParams::class)
	trackDuration: Duration
) = RecorderAppTheme {
	Surface {
		EditorAmplitudeGraph(
			playRatio = { .5f },
			totalTrackDuration = trackDuration,
			graphData = { PlayerPreviewFakes.loadAmplitudeGraph(trackDuration) },
			modifier = Modifier
				.padding(18.dp)
				.fillMaxWidth(),
		)
	}
}

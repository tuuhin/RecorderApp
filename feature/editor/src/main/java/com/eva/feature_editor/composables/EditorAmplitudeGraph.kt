package com.eva.feature_editor.composables

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.player_shared.util.PlayRatio
import com.eva.player_shared.util.PlayerGraphData
import com.eva.player_shared.util.PlayerPreviewFakes
import com.eva.player_shared.util.drawGraph
import com.eva.player_shared.util.drawTimeLine
import com.eva.player_shared.util.drawTrackPointer
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import com.eva.utils.RecorderConstants
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun EditorAmplitudeGraph(
	playRatio: PlayRatio,
	totalTrackDuration: Duration,
	graphData: PlayerGraphData,
	modifier: Modifier = Modifier,
	plotColor: Color = MaterialTheme.colorScheme.secondary,
	trackPointerColor: Color = MaterialTheme.colorScheme.primary,
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

	Surface(
		shape = shape,
		color = containerColor,
		modifier = modifier.aspectRatio(1.6f),
	) {
		Spacer(
			modifier = Modifier
				.padding(contentPadding)
				.defaultMinSize(minHeight = dimensionResource(id = R.dimen.line_graph_min_height))
				.drawWithCache {
					val centerYAxis = size.height / 2f

					val spikesWidth = size.width / RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
					val spikeSpace = (spikesWidth - 1.5.dp.toPx()).let { amt ->
						if (amt > 0f) amt else 2.dp.toPx()
					}

					val translate = size.width * playRatio()

					onDrawBehind {
						// take only n amount of points
						val samples = graphData().let { graphList ->
							if (graphList.size >= RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE)
								graphList.take(RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE)
							else graphList
						}

						drawGraph(
							waves = samples,
							centerYAxis = centerYAxis,
							spikesGap = spikeSpace,
							spikesWidth = spikesWidth,
							color = plotColor
						)

						// timeline
						drawTimeLine(
							duration = totalTrackDuration,
							textMeasurer = textMeasurer,
							spikesWidth = spikesWidth,
							outlineColor = timelineColor,
							outlineVariant = timelineColorVariant,
							textStyle = timelineTextStyle,
							textColor = timelineTextColor,
						)

						// draw the pointer
						drawTrackPointer(
							xAxis = translate,
							color = trackPointerColor,
							radius = spikesWidth,
							strokeWidth = spikesWidth,
						)
					}
				},
		)
	}
}

@PreviewLightDark
@Composable
private fun EditorAmplitudeGraphPreview() = RecorderAppTheme {
	EditorAmplitudeGraph(
		playRatio = { .25f },
		totalTrackDuration = 10.seconds,
		graphData = { PlayerPreviewFakes.PREVIEW_RECORDER_AMPLITUDES },
		modifier = Modifier.fillMaxWidth(),
	)
}

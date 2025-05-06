package com.eva.feature_player.composable

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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.util.PlayRatio
import com.eva.player_shared.util.PlayerGraphData
import com.eva.player_shared.util.PlayerPreviewFakes
import com.eva.player_shared.util.drawGraph
import com.eva.player_shared.util.drawTimeLineWithBookMarks
import com.eva.player_shared.util.drawTrackPointer
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import com.eva.utils.RecorderConstants
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

@Composable
internal fun PlayerAmplitudeGraph(
	totalTrackDuration: Duration,
	playRatio: PlayRatio,
	graphData: PlayerGraphData,
	modifier: Modifier = Modifier,
	bookMarkTimeStamps: ImmutableList<LocalTime> = persistentListOf(),
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

					val spikesWidth = size.width / RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
					val spikeSpace = (spikesWidth - 1.5.dp.toPx()).let { amt ->
						if (amt > 0f) amt else 2.dp.toPx()
					}

					onDrawBehind {
						val samples = graphData()
						val bookMarksAsMillis = bookMarkTimeStamps.map { it.toMillisecondOfDay() }

						val totalSize = samples.size * spikesWidth
						val translate = size.width * .5f - (totalSize * playRatio())

						translate(left = translate) {
							drawGraph(
								waves = samples,
								spikesGap = spikeSpace,
								spikesWidth = spikesWidth,
								color = plotColor
							)
							drawTimeLineWithBookMarks(
								totalDuration = totalTrackDuration,
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
							xAxis = center.x,
							color = trackPointerColor,
							radius = spikesWidth,
							strokeWidth = spikesWidth
						)
					}
				},
		)
	}
}

@Composable
fun PlayerAmplitudeGraph(
	trackData: PlayerTrackData,
	graphData: PlayerGraphData,
	modifier: Modifier = Modifier,
	bookMarksTimeStamps: ImmutableList<LocalTime> = persistentListOf(),
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
	PlayerAmplitudeGraph(
		playRatio = { trackData.playRatio },
		totalTrackDuration = trackData.total,
		graphData = graphData,
		bookMarkTimeStamps = bookMarksTimeStamps,
		modifier = modifier,
		plotColor = plotColor,
		trackPointerColor = trackPointerColor,
		bookMarkColor = bookMarkColor,
		timelineColor = timelineColor,
		timelineColorVariant = timelineColorVariant,
		timelineTextColor = timelineTextColor,
		timelineTextStyle = timelineTextStyle,
		contentPadding = contentPadding,
		shape = shape,
		containerColor = containerColor,
	)
}

@Preview
@Composable
private fun PlayerAmplitudeGraphPreview() = RecorderAppTheme {
	PlayerAmplitudeGraph(
		trackData = PlayerPreviewFakes.FAKE_TRACK_DATA,
		graphData = { PlayerPreviewFakes.PREVIEW_RECORDER_AMPLITUDES },
		bookMarksTimeStamps = persistentListOf(
			LocalTime.fromSecondOfDay(2),
			LocalTime.fromSecondOfDay(8)
		),
		modifier = Modifier.fillMaxWidth(),
	)
}

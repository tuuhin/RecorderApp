package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.player.model.PlayerTrackData
import com.eva.recorderapp.voice_recorder.domain.recorder.VoiceRecorder
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerGraphDrawUtil.drawGraph
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerGraphDrawUtil.drawTimeLine
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerGraphDrawUtil.drawTrackPointer
import com.eva.recorderapp.voice_recorder.presentation.util.PlayRation
import com.eva.recorderapp.voice_recorder.presentation.util.PlayerGraphData
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalTime
import kotlin.time.Duration.Companion.seconds


@Composable
fun PlayerAmplitudeGraph(
	totalTrackDuration: LocalTime,
	playRatio: PlayRation,
	graphData: PlayerGraphData,
	bookMarkTimeStamps: ImmutableList<LocalTime>,
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

					val spikesWidth = size.width / VoiceRecorder.RECORDER_AMPLITUDES_BUFFER_SIZE
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
								centerYAxis = centerYAxis,
								spikesGap = spikeSpace,
								spikesWidth = spikesWidth,
								color = plotColor
							)
							drawTimeLine(
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
	bookMarksTimeStamps: ImmutableList<LocalTime>,
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
	PlayerAmplitudeGraph(
		playRatio = { trackData.playRatio },
		totalTrackDuration = trackData.totalAsLocalTime,
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

@PreviewLightDark
@Composable
private fun PlayerAmplitudeGraphPreview() = RecorderAppTheme {
	PlayerAmplitudeGraph(
		trackData = PlayerTrackData(current = 5.seconds, total = 10.seconds),
		graphData = { PreviewFakes.PREVIEW_RECORDER_AMPLITUDES },
		bookMarksTimeStamps = persistentListOf(LocalTime.fromSecondOfDay(2), LocalTime.fromSecondOfDay(8)),
		modifier = Modifier.fillMaxWidth(),
	)
}

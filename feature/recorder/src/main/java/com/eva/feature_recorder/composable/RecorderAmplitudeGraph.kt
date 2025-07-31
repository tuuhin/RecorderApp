package com.eva.feature_recorder.composable

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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.feature_recorder.util.RecorderPreviewFakes
import com.eva.feature_recorder.util.drawAmplitudeGraph
import com.eva.feature_recorder.util.drawRecorderTimeline
import com.eva.recorder.utils.DeferredDurationList
import com.eva.recorder.utils.DeferredRecordedPointList
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import com.eva.utils.RecorderConstants
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun RecorderAmplitudeGraph(
	recoderPointsCallback: DeferredRecordedPointList,
	bookMarksDeferred: DeferredDurationList,
	modifier: Modifier = Modifier,
	chartColor: Color = MaterialTheme.colorScheme.secondary,
	bookMarkColor: Color = MaterialTheme.colorScheme.tertiary,
	timelineColor: Color = MaterialTheme.colorScheme.outline,
	timelineColorVariant: Color = MaterialTheme.colorScheme.outlineVariant,
	timelineTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
	timelineFontFamily: FontFamily = FontFamily.Monospace,
	timelineTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	shape: Shape = MaterialTheme.shapes.small,
	contentPadding: PaddingValues = PaddingValues(
		horizontal = dimensionResource(id = R.dimen.graph_card_padding),
		vertical = dimensionResource(R.dimen.graph_card_padding_other)
	),
) {
	val textMeasurer = rememberTextMeasurer()
	val bookMarkDrawable = painterResource(R.drawable.ic_bookmark)

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
					val blockSize = RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
					val centerYAxis = size.height / 2

					val spikesWidth = size.width / RecorderConstants.RECORDER_AMPLITUDES_BUFFER_SIZE
					val spikesGap = (spikesWidth - 1.5.dp.toPx()).let { amt ->
						if (amt >= 0f) amt else 2.dp.toPx()
					}

					onDrawBehind {

						val result = recoderPointsCallback()
						val actualResult = result.takeWhile { !it.isPaddingPoint }

						val rmsData = actualResult.map { it.rmsValue }
						val timelineInMillis = result.map { it.timeInMillis }

						val translateAmount = if (actualResult.size <= blockSize) 0f
						else (blockSize - actualResult.size) * spikesWidth

						val bookmarksToDraw = if (timelineInMillis.isNotEmpty()) {
							// min is required not to draw extra lines and max ensures it doesn't
							// cross the line
							val range = timelineInMillis.min()..<timelineInMillis.max()

							bookMarksDeferred()
								.filter { time -> time.inWholeMilliseconds in range }
								.map { it.inWholeMilliseconds }
						}
						// otherwise its empty
						else emptyList()

						// clipping ensure we don't draw extras
						clipRect(right = size.width + 5.dp.toPx()) {
							translate(left = translateAmount) {
								drawAmplitudeGraph(
									amplitudes = rmsData,
									spikesGap = spikesGap,
									centerYAxis = centerYAxis,
									spikesWidth = spikesWidth,
									barColor = chartColor,
								)
							}
						}
						translate(translateAmount) {
							drawRecorderTimeline(
								image = bookMarkDrawable,
								timelineInMillis = timelineInMillis,
								bookMarks = bookmarksToDraw,
								textMeasurer = textMeasurer,
								spikesWidth = spikesWidth,
								strokeWidthThick = 2.dp.toPx(),
								strokeWidthLight = 1.25.dp.toPx(),
								bookMarkColor = bookMarkColor,
								outlineColor = timelineColor,
								outlineVariant = timelineColorVariant,
								textStyle = timelineTextStyle.copy(fontFamily = timelineFontFamily),
								textColor = timelineTextColor
							)
						}
					}
				},
		)
	}
}


@PreviewLightDark
@Composable
private fun RecorderAmplitudeGraphPreview() = RecorderAppTheme {
	RecorderAmplitudeGraph(
		recoderPointsCallback = { RecorderPreviewFakes.PREVIEW_RECORDER_AMPLITUDE_FLOAT_ARRAY_LARGE },
		bookMarksDeferred = { listOf(2.seconds, 5.seconds) },
		modifier = Modifier.fillMaxWidth(),
	)
}

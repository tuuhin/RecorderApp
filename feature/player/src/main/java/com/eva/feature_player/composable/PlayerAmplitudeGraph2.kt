package com.eva.feature_player.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.eva.feature_player.views.PlayerAmplitudeGraph2View
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.util.PlayRatio
import com.eva.player_shared.util.PlayerGraphData
import com.eva.ui.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

@Composable
fun PlayerAmplitudeGraph2(
	totalTrackDuration: Duration,
	trackPlayRatio: PlayRatio,
	graphData: PlayerGraphData,
	modifier: Modifier = Modifier,
	isSwipeToScrollEnabled: Boolean = false,
	onSwipe: (ratio: Float) -> Unit = {},
	onSwipeEnd: () -> Unit = {},
	bookMarkTimeStamps: ImmutableList<LocalTime> = persistentListOf(),
	plotColor: Color = MaterialTheme.colorScheme.secondary,
	trackPointerColor: Color = MaterialTheme.colorScheme.primary,
	bookMarkColor: Color = MaterialTheme.colorScheme.tertiary,
	timelineColor: Color = MaterialTheme.colorScheme.outline,
	timelineColorVariant: Color = MaterialTheme.colorScheme.outlineVariant,
	timelineTextStyle: TextStyle = MaterialTheme.typography.labelSmall,
	timelineTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	contentPadding: PaddingValues = PaddingValues(20.dp),
) {
	val resolver = LocalFontFamilyResolver.current
	val density = LocalDensity.current
	val layoutDirection = LocalLayoutDirection.current

	val typeFace by remember(timelineTextStyle) {
		resolver.resolveAsTypeface(
			fontFamily = timelineTextStyle.fontFamily ?: FontFamily.Monospace,
			fontWeight = timelineTextStyle.fontWeight ?: FontWeight.Normal,
			fontStyle = timelineTextStyle.fontStyle ?: FontStyle.Normal,
			fontSynthesis = timelineTextStyle.fontSynthesis ?: FontSynthesis.None
		)
	}

	AndroidView(
		factory = { ctx ->
			PlayerAmplitudeGraph2View(ctx).also { view ->
				// colors and font
				view.plotColor = plotColor.toArgb()
				view.timelineColor = timelineColor.toArgb()
				view.timelineColorVariant = timelineColorVariant.toArgb()
				view.timelineTextColor = timelineTextColor.toArgb()
				view.bookMarkColor = bookMarkColor.toArgb()
				view.trackPointerColor = trackPointerColor.toArgb()
				view.canvasBackground = containerColor.toArgb()
				// text config
				view.textTypeface = typeFace
				with(density) {
					view.textFontSizeAsPx = timelineTextStyle.fontSize.toPx()
					view.setPadding(
						contentPadding.calculateLeftPadding(layoutDirection).roundToPx(),
						contentPadding.calculateTopPadding().roundToPx(),
						contentPadding.calculateRightPadding(layoutDirection).roundToPx(),
						contentPadding.calculateBottomPadding().roundToPx(),
					)
				}
				view.isSwipeToScrollEnabled = isSwipeToScrollEnabled
				// callbacks
				view.onSwipeToChangeEnd(onSwipeEnd)
				view.onSwipeToChangePlayPosition(onSwipe)
			}
		},
		update = { view ->
			// enable gesture detection
			view.isSwipeToScrollEnabled = isSwipeToScrollEnabled
			// if the view is visible then only show the values
			if (view.isShown) {
				view.onUpdateTrackDuration(totalTrackDuration)
				view.onUpdateBookMarks(bookMarkTimeStamps)
				view.onUpdatePlotColor(plotColor.toArgb())
				view.onUpdatePlayRatio { trackPlayRatio() }
				view.onUpdateGraphData { graphData() }
			}
		},
		onRelease = { view -> view.cleanUp() },
		modifier = modifier.defaultMinSize(minHeight = dimensionResource(id = R.dimen.line_graph_min_height))
	)
}

@Composable
internal fun PlayerAmplitudeGraph2(
	trackData: () -> PlayerTrackData,
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
	timelineFontFamily: FontFamily = FontFamily.Monospace,
	isSwipeToScrollEnabled: Boolean = false,
	onSeek: (Duration) -> Unit = {},
	onSeekEnd: () -> Unit = {},
	shape: Shape = MaterialTheme.shapes.small,
	contentPadding: PaddingValues = PaddingValues(
		horizontal = dimensionResource(id = R.dimen.graph_card_padding),
		vertical = dimensionResource(id = R.dimen.graph_card_padding_other)
	),
) {
	val totalDuration by remember { derivedStateOf { trackData().total } }

	Surface(
		shape = shape,
		color = containerColor,
		modifier = modifier.aspectRatio(1.6f)
	) {
		PlayerAmplitudeGraph2(
			trackPlayRatio = { trackData().playRatio },
			totalTrackDuration = totalDuration,
			graphData = graphData,
			bookMarkTimeStamps = bookMarksTimeStamps,
			isSwipeToScrollEnabled = isSwipeToScrollEnabled,
			onSwipe = { ratio ->
				val duration = trackData().calculateSeekAmount(ratio)
				onSeek(duration)
			},
			onSwipeEnd = onSeekEnd,
			plotColor = plotColor,
			trackPointerColor = trackPointerColor,
			bookMarkColor = bookMarkColor,
			timelineColor = timelineColor,
			timelineColorVariant = timelineColorVariant,
			timelineTextColor = timelineTextColor,
			timelineTextStyle = timelineTextStyle.copy(fontFamily = timelineFontFamily),
			contentPadding = contentPadding,
			containerColor = containerColor,
		)
	}
}

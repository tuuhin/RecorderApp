package com.eva.feature_recorder.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.eva.feature_recorder.util.RecorderPreviewFakes
import com.eva.recorder.domain.models.RecorderAction
import com.eva.recorder.domain.models.RecorderState
import com.eva.recorder.utils.DeferredDurationList
import com.eva.recorder.utils.DeferredRecordedPointList
import com.eva.ui.R
import com.eva.ui.theme.DownloadableFonts
import com.eva.ui.theme.RecorderAppTheme
import kotlinx.datetime.LocalTime

@Composable
internal fun RecorderContent(
	timer: LocalTime,
	recordingPointsCallback: DeferredRecordedPointList,
	bookMarksDeferred: DeferredDurationList,
	recorderState: RecorderState,
	onRecorderAction: (RecorderAction) -> Unit,
	modifier: Modifier = Modifier,
	isStartRecordingEnabled: Boolean = true,
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(40.dp),
			modifier = Modifier.offset(y = dimensionResource(id = R.dimen.graph_offset))
		) {
			RecorderTimerText(
				time = timer,
				fontFamily = DownloadableFonts.SPLINE_SANS_MONO_FONT_FAMILY
			)
			RecorderAmplitudeGraph(
				recoderPointsCallback = recordingPointsCallback,
				bookMarksDeferred = bookMarksDeferred,
				timelineFontFamily = DownloadableFonts.PLUS_CODE_LATIN_FONT_FAMILY,
				modifier = Modifier.fillMaxWidth(),
			)
		}
		AnimatedRecorderActionTray(
			recorderState = recorderState,
			onRecorderAction = onRecorderAction,
			isStartRecordingEnabled = isStartRecordingEnabled,
			modifier = Modifier
				.offset(y = dimensionResource(id = R.dimen.recordings_action_offset))
				.fillMaxWidth()
				.align(Alignment.BottomCenter)
		)
	}
}

private class RecorderContentRecorderStatePreviewParams :
	CollectionPreviewParameterProvider<RecorderState>(
		listOf(
			RecorderState.RECORDING,
			RecorderState.COMPLETED,
			RecorderState.PAUSED
		)
	)

@PreviewLightDark
@Composable
private fun RecorderContentPreview(
	@PreviewParameter(RecorderContentRecorderStatePreviewParams::class)
	recorderState: RecorderState,
) = RecorderAppTheme {
	Surface {
		RecorderContent(
			timer = LocalTime(0, 10, 56, 0),
			recorderState = recorderState,
			bookMarksDeferred = { listOf() },
			recordingPointsCallback = { RecorderPreviewFakes.PREVIEW_RECORDER_AMPLITUDE_FLOAT_ARRAY_LARGE },
			onRecorderAction = {},
			modifier = Modifier
				.padding(horizontal = dimensionResource(R.dimen.sc_padding_secondary))
				.fillMaxSize()
		)
	}
}
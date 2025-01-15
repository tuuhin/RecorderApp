package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

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
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import com.eva.recorderapp.voice_recorder.presentation.util.BookMarksDeferredCallback
import com.eva.recorderapp.voice_recorder.presentation.util.PreviewFakes
import com.eva.recorderapp.voice_recorder.presentation.util.RecordingDataPointCallback
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalTime

@Composable
fun RecorderContent(
	timer: LocalTime,
	recordingPointsCallback: RecordingDataPointCallback,
	bookMarksDeferred: BookMarksDeferredCallback,
	recorderState: RecorderState,
	onRecorderAction: (RecorderAction) -> Unit,
	modifier: Modifier = Modifier,
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
			RecorderTimerText(time = timer)
			RecorderAmplitudeGraph(
				amplitudesCallback = recordingPointsCallback,
				bookMarksDeferred = bookMarksDeferred,
				modifier = Modifier.fillMaxWidth(),
			)
		}
		AnimatedRecorderActionTray(
			recorderState = recorderState,
			onRecorderAction = onRecorderAction,
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
			bookMarksDeferred = { persistentListOf() },
			recordingPointsCallback = { PreviewFakes.PREVIEW_RECORDER_AMPLITUDES_FLOAT_ARRAY },
			onRecorderAction = {},
			modifier = Modifier
				.padding(horizontal = dimensionResource(R.dimen.sc_padding_secondary))
				.fillMaxSize()
		)
	}
}
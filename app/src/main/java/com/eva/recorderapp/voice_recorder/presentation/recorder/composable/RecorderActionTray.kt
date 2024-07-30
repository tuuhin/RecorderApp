package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.domain.emums.RecorderState

@Composable
fun AnimatedRecorderActionTray(
	recorderState: RecorderState,
	onRecorderAction: (RecorderAction) -> Unit,
	modifier: Modifier = Modifier
) {
	AnimatedContent(
		targetState = recorderState,
		transitionSpec = { recorderStateAnimation() },
		modifier = modifier.defaultMinSize(minHeight = 120.dp),
		contentAlignment = Alignment.Center
	) { state ->

		when (state) {
			RecorderState.IDLE, RecorderState.COMPLETED, RecorderState.CANCELLED -> {
				Box(contentAlignment = Alignment.Center) {
					RecordButton(
						onClick = { onRecorderAction(RecorderAction.START_RECORDER) },
					)
				}
			}

			RecorderState.RECORDING, RecorderState.PAUSED -> {
				RecorderPausePlayAction(
					state = state,
					onResume = { onRecorderAction(RecorderAction.RESUME_RECORDER) },
					onPause = { onRecorderAction(RecorderAction.PAUSE_RECORDER) },
					onCancel = { onRecorderAction(RecorderAction.CANCEL_RECORDER) },
					onStop = { onRecorderAction(RecorderAction.STOP_RECORDER) },
					modifier = Modifier.fillMaxWidth()
				)
			}

			else -> Box(contentAlignment = Alignment.Center) {
				AssistChip(
					onClick = {},
					label = {
						Text(
							text = stringResource(id = R.string.recorder_state_preparing),
							style = MaterialTheme.typography.labelLarge
						)
					},
					colors = AssistChipDefaults.elevatedAssistChipColors(
						containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
					)
				)
			}
		}
	}
}

private fun AnimatedContentTransitionScope<RecorderState>.recorderStateAnimation(): ContentTransform {
	return fadeIn() + expandIn(expandFrom = Alignment.Center) togetherWith
			fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
}

private class RecorderStatePreviewParams : CollectionPreviewParameterProvider<RecorderState>(
	listOf(
		RecorderState.RECORDING,
		RecorderState.COMPLETED,
		RecorderState.PAUSED,
		RecorderState.PREPARING
	)
)

@PreviewLightDark
@Composable
private fun AnimatedRecorderActionTrayPreview(
	@PreviewParameter(RecorderStatePreviewParams::class)
	state: RecorderState
) = RecorderAppTheme {
	Surface {
		AnimatedRecorderActionTray(
			recorderState = state,
			onRecorderAction = {},
			modifier = Modifier
				.padding(horizontal = 4.dp)
				.fillMaxWidth()
		)
	}
}
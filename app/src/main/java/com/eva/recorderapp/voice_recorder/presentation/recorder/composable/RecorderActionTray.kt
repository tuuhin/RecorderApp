package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderAction
import com.eva.recorderapp.voice_recorder.domain.recorder.emums.RecorderState
import com.eva.recorderapp.voice_recorder.presentation.recorder.util.RecorderActionMode
import com.eva.recorderapp.voice_recorder.presentation.recorder.util.toAction

@Composable
fun AnimatedRecorderActionTray(
	recorderState: RecorderState,
	onRecorderAction: (RecorderAction) -> Unit,
	modifier: Modifier = Modifier
) {
	val mode by remember(recorderState) {
		derivedStateOf(recorderState::toAction)
	} 

	AnimatedContent(
		targetState = mode,
		transitionSpec = { recorderStateAnimation() },
		modifier = modifier.defaultMinSize(minHeight = 120.dp),
		contentAlignment = Alignment.Center,
		label = "Recorder Action Animation"
	) { state ->

		when (state) {
			RecorderActionMode.INIT -> {
				Box(contentAlignment = Alignment.Center) {
					RecordButton(
						onClick = { onRecorderAction(RecorderAction.StartRecorderAction) },
					)
				}
			}

			RecorderActionMode.RECORDING -> {
				RecorderPausePlayAction(
					showPausedAction = recorderState == RecorderState.PAUSED,
					onResume = { onRecorderAction(RecorderAction.ResumeRecorderAction) },
					onPause = { onRecorderAction(RecorderAction.PauseRecorderAction) },
					onCancel = { onRecorderAction(RecorderAction.CancelRecorderAction) },
					onStop = { onRecorderAction(RecorderAction.StopRecorderAction) },
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

private fun AnimatedContentTransitionScope<RecorderActionMode>.recorderStateAnimation(): ContentTransform {
	return fadeIn(
		animationSpec = tween(500)
	) + scaleIn(
		animationSpec = spring(
			dampingRatio = Spring.DampingRatioMediumBouncy,
			stiffness = Spring.StiffnessMediumLow,
		),
	) togetherWith fadeOut(
		animationSpec = tween(500)
	) + scaleOut(
		animationSpec = spring(
			dampingRatio = Spring.DampingRatioMediumBouncy,
			stiffness = Spring.StiffnessMediumLow,
		),
	) using SizeTransform(clip = false)
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
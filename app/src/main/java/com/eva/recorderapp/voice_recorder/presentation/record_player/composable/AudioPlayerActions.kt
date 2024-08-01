package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoNotDisturbOnTotalSilence
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.presentation.composables.IconButtonWithText

@Composable
fun AudioPlayerActions(
	isPlaying: Boolean,
	onPause: () -> Unit,
	onPlay: () -> Unit,
	modifier: Modifier = Modifier,
	onForwardBy1s: () -> Unit = {},
	onRewindBy1s: () -> Unit = {},
	shape: Shape = MaterialTheme.shapes.large,
	color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	contentColor: Color = contentColorFor(backgroundColor = color)
) {

	Surface(
		modifier = modifier,
		shape = shape,
		color = color,
		contentColor = contentColor
	) {
		Column(
			modifier = Modifier
				.padding(all = dimensionResource(id = R.dimen.player_options_card_padding)),
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(24.dp)
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				IconButton(onClick = {}) {
					Icon(
						imageVector = Icons.Default.DoNotDisturbOnTotalSilence,
						contentDescription = null
					)
				}
				IconButton(onClick = { /*TODO*/ }) {
					Icon(imageVector = Icons.Default.Repeat, contentDescription = null)
				}
				IconButton(onClick = {}) {
					Icon(imageVector = Icons.Default.DoubleArrow, contentDescription = null)
				}
			}
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				IconButtonWithText(
					icon = {
						Icon(
							painter = painterResource(id = R.drawable.ic_fast_rewind),
							contentDescription = stringResource(id = R.string.player_fast_forward)
						)
					},
					text = stringResource(id = R.string.player_fast_rewind),
					onClick = onRewindBy1s,
				)
				FloatingActionButton(
					onClick = {
						if (isPlaying) onPause()
						else onPlay()
					},
					elevation = FloatingActionButtonDefaults.loweredElevation(),
					contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
					containerColor = MaterialTheme.colorScheme.secondaryContainer
				) {
					Crossfade(targetState = isPlaying) { playing ->
						if (playing)
							Icon(
								painter = painterResource(id = R.drawable.ic_pause),
								contentDescription = stringResource(R.string.recorder_action_pause)
							)
						else Icon(
							painter = painterResource(id = R.drawable.ic_play),
							contentDescription = stringResource(R.string.recorder_action_resume)
						)
					}
				}
				IconButtonWithText(
					icon = {
						Icon(
							painter = painterResource(id = R.drawable.ic_fast_forward),
							contentDescription = stringResource(id = R.string.player_fast_forward)
						)
					},
					text = stringResource(id = R.string.player_fast_forward),
					onClick = onForwardBy1s,
				)
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun AudioPlayerActionsPreview() = RecorderAppTheme {
	AudioPlayerActions(isPlaying = true, onPlay = {}, onPause = {})
}
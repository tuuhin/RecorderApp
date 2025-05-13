package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.editor.domain.model.AudioEditAction
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.composables.AnimatedPlayPauseButton
import com.eva.player_shared.composables.PlayerSlider
import com.eva.ui.R
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorActionsAndControls(
	trackData: PlayerTrackData,
	onSeek: (Duration) -> Unit,
	isItemPlaying: Boolean,
	modifier: Modifier = Modifier,
	onCropMedia: () -> Unit,
	onCutMedia: () -> Unit,
	onPlay: () -> Unit,
	onPause: () -> Unit,
	playButtonColors: ButtonColors = ButtonDefaults.buttonColors(
		containerColor = MaterialTheme.colorScheme.primary,
		contentColor = MaterialTheme.colorScheme.onPrimary
	),
	actionButtonColors: ButtonColors = ButtonDefaults.buttonColors(
		containerColor = MaterialTheme.colorScheme.secondary,
		contentColor = MaterialTheme.colorScheme.onSecondary
	)
) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(40.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		PlayerSlider(trackData = trackData, onSeekComplete = onSeek)
		//actions
		Row(
			horizontalArrangement = Arrangement.spacedBy(40.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			// cut option
			TooltipBox(
				positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
				tooltip = {
					RichTooltip(
						title = {
							Text(
								text = stringResource(R.string.action_cut),
								style = MaterialTheme.typography.titleSmall,
								color = MaterialTheme.colorScheme.primary
							)
						},
						text = { Text(text = stringResource(R.string.tooltip_text_editor_cut)) },
						shape = MaterialTheme.shapes.medium,
						caretSize = TooltipDefaults.caretSize
					)
				},
				state = rememberTooltipState()
			) {
				SmallFloatingActionButton(
					onClick = onCutMedia,
					shape = CircleShape,
					containerColor = actionButtonColors.containerColor,
					contentColor = actionButtonColors.contentColor,
					elevation = FloatingActionButtonDefaults.loweredElevation()
				) {
					Icon(
						painter = painterResource(R.drawable.ic_cut),
						contentDescription = "Action Cut"
					)
				}
			}
			AnimatedPlayPauseButton(
				isPlaying = isItemPlaying,
				onPlay = onPlay,
				onPause = onPause,
				colors = playButtonColors,
			)

			//crop option
			TooltipBox(
				positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
				tooltip = {
					RichTooltip(
						title = {
							Text(
								text = stringResource(R.string.action_crop),
								style = MaterialTheme.typography.titleSmall,
								color = MaterialTheme.colorScheme.primary
							)
						},
						text = { Text(text = stringResource(R.string.tooltip_text_editor_crop)) },
						shape = MaterialTheme.shapes.medium,
					)
				},
				state = rememberTooltipState()
			) {
				SmallFloatingActionButton(
					onClick = onCropMedia,
					shape = CircleShape,
					containerColor = actionButtonColors.containerColor,
					contentColor = actionButtonColors.contentColor,
					elevation = FloatingActionButtonDefaults.loweredElevation()
				) {
					Icon(
						imageVector = Icons.Default.Crop,
						contentDescription = "Action Crop"
					)
				}
			}
		}
	}
}

@Composable
internal fun EditorActionsAndControls(
	trackData: PlayerTrackData,
	isMediaPlaying: Boolean,
	onEvent: (EditorScreenEvent) -> Unit,
	modifier: Modifier = Modifier,
) {
	EditorActionsAndControls(
		isItemPlaying = isMediaPlaying,
		trackData = trackData,
		modifier = modifier,
		onSeek = { onEvent(EditorScreenEvent.OnSeekTrack(it)) },
		onPlay = { onEvent(EditorScreenEvent.PlayAudio) },
		onPause = { onEvent(EditorScreenEvent.PauseAudio) },
		onCropMedia = { onEvent(EditorScreenEvent.OnEditAction(AudioEditAction.CROP)) },
		onCutMedia = { onEvent(EditorScreenEvent.OnEditAction(AudioEditAction.CUT)) }
	)
}
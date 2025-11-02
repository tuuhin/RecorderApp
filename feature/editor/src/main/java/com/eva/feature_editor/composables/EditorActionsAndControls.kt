package com.eva.feature_editor.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eva.editor.domain.model.AudioEditAction
import com.eva.feature_editor.event.EditorScreenEvent
import com.eva.player.domain.model.PlayerTrackData
import com.eva.player_shared.composables.AnimatedPlayPauseButton
import com.eva.player_shared.composables.PlayerTrackSlider2
import com.eva.ui.R
import kotlin.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorActionsAndControls(
	trackData: () -> PlayerTrackData,
	onSeek: (Duration) -> Unit,
	isItemPlaying: Boolean,
	modifier: Modifier = Modifier,
	onCropMedia: () -> Unit,
	onCutMedia: () -> Unit,
	onPlay: () -> Unit,
	onPause: () -> Unit,
	playButtonColor: Color = MaterialTheme.colorScheme.primary,
	actionButtonColor: Color = MaterialTheme.colorScheme.secondary,
) {
	Column(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(40.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		PlayerTrackSlider2(
			trackData = trackData,
			onSeekComplete = onSeek
		)
		//actions
		Row(
			horizontalArrangement = Arrangement.spacedBy(40.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			SecondaryActions(
				title = stringResource(R.string.action_cut),
				text = stringResource(R.string.tooltip_text_editor_cut),
				onClick = onCutMedia,
				containerColor = actionButtonColor,
			) {
				Icon(
					painter = painterResource(R.drawable.ic_cut),
					contentDescription = "Action Cut"
				)
			}
			AnimatedPlayPauseButton(
				isPlaying = isItemPlaying,
				onPlay = onPlay,
				onPause = onPause,
				containerColor = playButtonColor
			)
			SecondaryActions(
				title = stringResource(R.string.action_crop),
				text = stringResource(R.string.tooltip_text_editor_crop),
				onClick = onCropMedia,
				containerColor = actionButtonColor,
			) {
				Icon(
					painter = painterResource(R.drawable.ic_crop),
					contentDescription = "Action Crop"
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecondaryActions(
	title: String,
	text: String,
	onClick: () -> Unit,
	containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
	contentDescription: String? = null,
	action: @Composable () -> Unit,
) {
	Column(
		verticalArrangement = Arrangement.spacedBy(4.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = Modifier.semantics {
			this.contentDescription = contentDescription ?: title
		}
	) {
		//crop option
		TooltipBox(
			positionProvider = TooltipDefaults
				.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
			tooltip = {
				RichTooltip(
					title = { Text(text = title) },
					text = { Text(text = text) },
					colors = TooltipDefaults.richTooltipColors(
						titleContentColor = MaterialTheme.colorScheme.tertiary,
						contentColor = MaterialTheme.colorScheme.onSurface
					),
					shape = MaterialTheme.shapes.medium,
				)
			},
			state = rememberTooltipState(),
		) {
			SmallFloatingActionButton(
				onClick = onClick,
				shape = CircleShape,
				containerColor = containerColor,
				contentColor = contentColorFor(containerColor),
				elevation = FloatingActionButtonDefaults.loweredElevation()
			) {
				action()
			}
		}
		Text(
			text = title,
			style = MaterialTheme.typography.labelMedium
		)
	}
}

@Composable
internal fun EditorActionsAndControls(
	trackData: () -> PlayerTrackData,
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
		onCutMedia = { onEvent(EditorScreenEvent.OnEditAction(AudioEditAction.CUT)) })
}
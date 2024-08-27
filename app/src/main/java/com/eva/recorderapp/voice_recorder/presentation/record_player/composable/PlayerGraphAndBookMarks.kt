package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import com.eva.recorderapp.voice_recorder.presentation.util.PlayerGraphData

@Composable
fun PlayerGraphAndBookMarks(
	trackData: PlayerTrackData,
	graphData: PlayerGraphData,
	isGraphMode: Boolean,
	onToggleListAndWave: () -> Unit,
	onAddBookMark: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(4.dp),
		modifier = modifier,
	) {
		PlayerAmplitudeGraph(
			trackData = trackData,
			graphData = graphData,
			modifier = Modifier
				.aspectRatio(16f / 9f)
				.fillMaxWidth()
		)
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			AssistChip(
				onClick = onToggleListAndWave,
				label = { Text(text = stringResource(id = R.string.player_action_show_bookmarks)) },
				leadingIcon = {
					Icon(
						imageVector = Icons.AutoMirrored.Outlined.Label,
						contentDescription = stringResource(id = R.string.player_action_show_bookmarks)
					)
				},
				shape = MaterialTheme.shapes.large,
				colors = AssistChipDefaults.assistChipColors(
					containerColor = MaterialTheme.colorScheme.secondaryContainer,
					labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
					leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
				)
			)
			SuggestionChip(
				onClick = onAddBookMark,
				label = { Text(text = stringResource(id = R.string.player_action_add_bookmark)) },
				icon = {
					Icon(
						painter = painterResource(id = R.drawable.ic_bookmark),
						contentDescription = stringResource(id = R.string.player_action_add_bookmark),
					)
				},
				enabled = isGraphMode,
				shape = MaterialTheme.shapes.large,
				colors = SuggestionChipDefaults.suggestionChipColors(
					containerColor = MaterialTheme.colorScheme.tertiaryContainer,
					labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
					iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer
				)
			)
		}
	}
}
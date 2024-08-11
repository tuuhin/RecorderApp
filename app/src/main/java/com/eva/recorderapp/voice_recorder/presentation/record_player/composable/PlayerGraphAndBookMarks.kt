package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.player.PlayerTrackData
import com.eva.recorderapp.voice_recorder.presentation.record_player.util.PlayerGraphInfo

@Composable
fun PlayerGraphAndBookMarks(
	trackData: PlayerTrackData,
	graphData: PlayerGraphInfo,
	isGraphMode: Boolean,
	onToggleListAndWave: () -> Unit,
	onAddBookMark: () -> Unit,
	modifier: Modifier = Modifier
) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		modifier = modifier
	) {
		PlayerAmplitudeGraph(
			trackData = trackData,
			samples = graphData.waves,
			modifier = Modifier
				.aspectRatio(1.5f)
				.fillMaxWidth()
		)
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			TextButton(onClick = onToggleListAndWave) {
				Icon(
					imageVector = Icons.AutoMirrored.Outlined.Label,
					contentDescription = stringResource(id = R.string.player_action_show_bookmarks)
				)
				Spacer(modifier = Modifier.width(2.dp))
				Text(text = stringResource(id = R.string.player_action_show_bookmarks))
			}
			TextButton(
				onClick = onAddBookMark,
				enabled = isGraphMode
			) {
				Icon(
					painter = painterResource(id = R.drawable.ic_bookmark),
					contentDescription = stringResource(id = R.string.player_action_add_bookmark),
					tint = MaterialTheme.colorScheme.surfaceTint
				)
				Spacer(modifier = Modifier.width(2.dp))
				Text(text = stringResource(id = R.string.player_action_add_bookmark))
			}
		}
	}
}
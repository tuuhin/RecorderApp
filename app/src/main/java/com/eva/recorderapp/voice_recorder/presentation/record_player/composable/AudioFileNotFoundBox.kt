package com.eva.recorderapp.voice_recorder.presentation.record_player.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun AudioFileNotFoundBox(modifier: Modifier = Modifier) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(4.dp)
	) {
		Image(
			painter = painterResource(id = R.drawable.ic_music_file),
			contentDescription = stringResource(id = R.string.music_file_not_found_title),
			colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.secondary),
			modifier = Modifier.size(64.dp)
		)
		Text(
			text = stringResource(id = R.string.music_file_not_found_title),
			style = MaterialTheme.typography.bodyLarge,
			color = MaterialTheme.colorScheme.onBackground
		)
		Text(
			text = stringResource(id = R.string.music_file_not_found_desc),
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onBackground
		)
	}
}

@PreviewLightDark
@Composable
private fun AudioFileNotFoundBoxPreview() = RecorderAppTheme {
	Surface {
		AudioFileNotFoundBox(modifier = Modifier.fillMaxWidth())
	}
}
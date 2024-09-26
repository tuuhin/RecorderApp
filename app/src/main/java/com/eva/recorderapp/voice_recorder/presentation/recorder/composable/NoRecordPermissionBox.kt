package com.eva.recorderapp.voice_recorder.presentation.recorder.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme

@Composable
fun NoRecordPermissionBox(
	onPermissionChanged: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		Image(
			painter = painterResource(id = R.drawable.no_mic_allowed),
			contentDescription = stringResource(id = R.string.recorder_permission_not_found),
			colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
			modifier = Modifier.size(120.dp)
		)
		Text(
			text = stringResource(id = R.string.recorder_permission_not_found),
			style = MaterialTheme.typography.titleMedium
		)
		Text(
			text = stringResource(id = R.string.recorder_permission_not_found_desc),
			style = MaterialTheme.typography.bodyMedium,
			textAlign = TextAlign.Center
		)
		Spacer(modifier = Modifier.height(2.dp))
		CheckPermissionButton(
			onPermissionChanged = onPermissionChanged,
			shape = MaterialTheme.shapes.medium
		)
	}
}

@PreviewLightDark
@Composable
private fun NoRecorderPermissionBox() = RecorderAppTheme {
	Surface {
		NoRecordPermissionBox(
			onPermissionChanged = {},
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp)
		)
	}
}
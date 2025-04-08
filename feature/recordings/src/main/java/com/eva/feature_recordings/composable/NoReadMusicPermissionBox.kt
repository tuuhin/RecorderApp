package com.eva.feature_recordings.composable

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun NoReadMusicPermissionBox(
	onPermissionChanged: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
) {

	val context = LocalContext.current

	var hasExternalStoragePermission by remember {
		val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
		else
			ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)

		mutableStateOf(perms == PermissionChecker.PERMISSION_GRANTED)
	}


	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { isGranted ->
		hasExternalStoragePermission = isGranted
		onPermissionChanged(hasExternalStoragePermission)
	}

	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.spacedBy(12.dp)
	) {
		Image(
			painter = painterResource(id = R.drawable.ic_music_lib),
			contentDescription = stringResource(id = R.string.recordings_permissions_not_found),
			colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
			modifier = Modifier.size(120.dp)
		)
		Text(
			text = stringResource(id = R.string.recordings_permissions_not_found),
			style = MaterialTheme.typography.titleMedium
		)
		Text(
			text = stringResource(id = R.string.recordings_permissions_not_found_desc),
			style = MaterialTheme.typography.bodyMedium,
			textAlign = TextAlign.Center
		)
		Button(
			onClick = {

				val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
					Manifest.permission.READ_MEDIA_AUDIO
				else Manifest.permission.READ_EXTERNAL_STORAGE

				launcher.launch(perms)
			},
			shape = MaterialTheme.shapes.medium,
		) {
			Text(text = stringResource(id = R.string.allow_permissions))
		}
	}
}

@PreviewLightDark
@Composable
private fun NoReadMusicPermissionBoxPreview() = RecorderAppTheme {
	Surface {
		NoReadMusicPermissionBox(
			onPermissionChanged = {},
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp)
		)
	}
}
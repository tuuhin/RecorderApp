package com.eva.recorderapp.voice_recorder.presentation.settings.composables.audio

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.settings.composables.SettingsItemWithSwitch

@Composable
fun PauseRecorderOnCallTile(
	canPause: Boolean,
	onChange: (Boolean) -> Unit,
	modifier: Modifier = Modifier
) {

	val context = LocalContext.current

	var hasPermission by remember(context) {
		mutableStateOf(
			ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
					== PermissionChecker.PERMISSION_GRANTED
		)
	}

	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { isGranted ->
		hasPermission = isGranted
	}

	val isSelected by remember(hasPermission, canPause) {
		derivedStateOf { hasPermission && canPause }
	}

	SettingsItemWithSwitch(
		isSelected = isSelected,
		title = stringResource(id = R.string.recording_settings_pase_recorder_on_calls),
		text = stringResource(id = R.string.recording_settings_pause_recorder_on_call_text),
		leading = {
			Icon(
				painter = painterResource(id = R.drawable.ic_call),
				contentDescription = stringResource(id = R.string.recording_settings_pase_recorder_on_calls),
			)
		},
		modifier = modifier,
		onSelect = {
			if (!hasPermission) {
				launcher.launch(Manifest.permission.READ_PHONE_STATE)
			} else {
				onChange(it)
			}
		},
	)
}
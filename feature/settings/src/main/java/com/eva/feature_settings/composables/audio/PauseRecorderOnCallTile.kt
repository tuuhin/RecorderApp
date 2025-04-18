package com.eva.feature_settings.composables.audio

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
import com.eva.feature_settings.composables.SettingsItemWithSwitch
import com.eva.ui.R

@Composable
internal fun PauseRecorderOnCallTile(
	isPauseRecordingOnIncommingCall: Boolean,
	onActionEnabledChanged: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
) {
	val context = LocalContext.current
	var showDialog by remember { mutableStateOf(false) }

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

	PauseCallInfoDialog(
		showDialog = showDialog && !hasPermission,
		onConfirm = { launcher.launch(Manifest.permission.READ_PHONE_STATE) },
		onDismiss = { showDialog = false },
	)

	SettingsItemWithSwitch(
		isSelected = hasPermission && isPauseRecordingOnIncommingCall,
		title = stringResource(id = R.string.recording_settings_pause_recorder_on_calls),
		text = stringResource(id = R.string.recording_settings_pause_recorder_on_call_text),
		leading = {
			Icon(
				painter = painterResource(id = R.drawable.ic_call),
				contentDescription = stringResource(id = R.string.recording_settings_pause_recorder_on_calls),
			)
		},
		modifier = modifier,
		onSelect = { isEnabled ->
			if (!hasPermission) showDialog = true
			else onActionEnabledChanged(isEnabled)
		},
	)
}
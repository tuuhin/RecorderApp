package com.eva.feature_settings.composables.audio

import android.Manifest
import android.content.Context
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
internal fun LocationInfoCollectionCard(
	isAddLocationInfoAllowed: Boolean,
	onActionEnabledChanged: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
) {
	val context = LocalContext.current
	var showDialog by remember { mutableStateOf(false) }

	var hasPermission by remember(context) {
		mutableStateOf(context.hasLocationPermission)
	}

	val launcher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.RequestPermission()
	) { isGranted ->
		hasPermission = isGranted
	}

	LocationInfoDialog(
		showDialog = showDialog && !hasPermission,
		onConfirm = { launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
		onDismiss = { showDialog = false },
	)

	SettingsItemWithSwitch(
		isSelected = hasPermission && isAddLocationInfoAllowed,
		title = stringResource(id = R.string.recording_settings_add_location_info_title),
		text = stringResource(id = R.string.recording_settings_add_location_info_text),
		leading = {
			Icon(
				painter = painterResource(id = R.drawable.ic_location),
				contentDescription = stringResource(id = R.string.recording_settings_add_location_info_title),
			)
		},
		modifier = modifier,
		onSelect = { isEnabled ->
			if (!hasPermission) showDialog = true
			else onActionEnabledChanged(isEnabled)
		},
	)
}

private val Context.hasLocationPermission: Boolean
	get() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
			PermissionChecker.PERMISSION_GRANTED
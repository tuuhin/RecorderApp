package com.eva.feature_settings.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eva.ui.R

internal enum class SettingsTabs(
	val tabIndex: Int,
) {
	AUDIO_SETTINGS(0),
	FILES_SETTINGS(1);

	val textRes: String
		@Composable
		get() = when (this) {
			AUDIO_SETTINGS -> stringResource(R.string.app_settings_audio)
			FILES_SETTINGS -> stringResource(R.string.app_settings_files)
		}
}
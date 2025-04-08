package com.eva.feature_settings.utils

import androidx.annotation.StringRes
import com.eva.ui.R

internal enum class SettingsTabs(
	val tabIndex: Int,
	@StringRes val stringRes: Int,
) {
	AUDIO_SETTINGS(0, R.string.app_settings_audio),
	FILES_SETTINGS(1, R.string.app_settings_files)
}
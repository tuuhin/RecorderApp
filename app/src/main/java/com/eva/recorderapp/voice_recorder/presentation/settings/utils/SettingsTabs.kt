package com.eva.recorderapp.voice_recorder.presentation.settings.utils

import androidx.annotation.StringRes
import com.eva.recorderapp.R

enum class SettingsTabs(
	val tabIndex: Int,
	@StringRes val stringRes: Int
) {
	AUDIO_SETTINGS(0, R.string.app_settings_audio),
	FILES_SETTINGS(1, R.string.app_settings_files)
}
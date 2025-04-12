package com.eva.feature_settings.composables.files

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eva.datastore.domain.enums.AudioFileNamingFormat
import com.eva.ui.R

@Composable
internal fun AudioFileNamingFormat.stringRes(prefix: String): String = when (this) {
	AudioFileNamingFormat.DATE_TIME -> stringResource(
		id = R.string.recording_settings_name_format_date_time,
		prefix
	)

	AudioFileNamingFormat.COUNT -> stringResource(
		id = R.string.recording_settings_name_format_count,
		prefix
	)
}
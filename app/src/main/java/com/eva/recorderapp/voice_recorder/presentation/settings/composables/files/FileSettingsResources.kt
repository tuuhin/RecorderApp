package com.eva.recorderapp.voice_recorder.presentation.settings.composables.files

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.domain.datastore.enums.AudioFileNamingFormat

@Composable
fun AudioFileNamingFormat.stringRes(name: String): String = when (this) {
	AudioFileNamingFormat.DATE_TIME -> stringResource(id = R.string.recording_settings_name_format_date_time, name)
	AudioFileNamingFormat.COUNT -> stringResource(id = R.string.recording_settings_name_format_count, name)
}
package com.eva.recorderapp.voice_recorder.presentation.settings.composables.files

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderFileSettings
import com.eva.recorderapp.voice_recorder.presentation.settings.utils.FileSettingsEvent

@Composable
fun FileSettings(
	settings: RecorderFileSettings,
	onEvent: (FileSettingsEvent) -> Unit,
	modifier: Modifier = Modifier
) {
	LazyColumn(
		modifier = modifier.fillMaxSize(),
	) {

	}
}

@PreviewLightDark
@Composable
private fun AppFileSettingsPreview() = RecorderAppTheme {
	Surface {
		FileSettings(
			settings = RecorderFileSettings(),
			onEvent = {},
		)
	}
}
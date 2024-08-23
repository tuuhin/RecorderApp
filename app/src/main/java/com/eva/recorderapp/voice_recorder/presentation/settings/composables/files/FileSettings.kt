package com.eva.recorderapp.voice_recorder.presentation.settings.composables.files

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderFileSettings
import com.eva.recorderapp.voice_recorder.presentation.settings.utils.FileSettingsChangeEvent

@Composable
fun FileSettings(
	settings: RecorderFileSettings,
	onEvent: (FileSettingsChangeEvent) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp)
) {
	LazyColumn(
		modifier = modifier.fillMaxSize(),
		contentPadding = contentPadding
	) {
		item {
			StorageStatistics(modifier = Modifier.fillMaxWidth())
		}
		item {
			FilePrefixSelector(
				prefix = settings.name,
				onPrefixChange = { onEvent(FileSettingsChangeEvent.OnRecordingPrefixChange(it)) }
			)
		}
		item {
			FileNamingFormat(
				prefix = settings.name,
				format = settings.format,
				onFormatChange = { onEvent(FileSettingsChangeEvent.OnFormatChange(it)) }
			)
		}
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
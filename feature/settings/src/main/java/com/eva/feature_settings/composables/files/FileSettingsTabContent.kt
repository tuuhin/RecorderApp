package com.eva.feature_settings.composables.files

import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.datastore.domain.models.RecorderFileSettings
import com.eva.feature_settings.composables.SettingsItemWithSwitch
import com.eva.feature_settings.utils.FileSettingsChangeEvent
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun FileSettingsTabContent(
	settings: RecorderFileSettings,
	onEvent: (FileSettingsChangeEvent) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(0.dp),
) {
	LazyColumn(
		modifier = modifier.fillMaxSize(),
		contentPadding = contentPadding
	) {
		item {
			StorageStatistics(modifier = Modifier.fillMaxWidth())
		}
		item {
			FileNamingFormat(
				prefix = settings.name,
				format = settings.format,
				onFormatChange = { onEvent(FileSettingsChangeEvent.OnFormatChange(it)) }
			)
		}
		item {
			FilePrefixSelector(
				prefix = settings.name,
				onPrefixChange = { onEvent(FileSettingsChangeEvent.OnRecordingPrefixChange(it)) }
			)
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			item {
				SettingsItemWithSwitch(
					isSelected = settings.allowExternalRead,
					title = stringResource(id = R.string.recording_settings_file_allow_external_read),
					text = stringResource(id = R.string.recording_settings_file_allow_external_read_text),
					onSelect = { isEnabled ->
						onEvent(FileSettingsChangeEvent.OnAllowExternalFiles(isEnabled))
					},
				)
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun AppFileSettingsTabContentPreview() = RecorderAppTheme {
	Surface {
		FileSettingsTabContent(
			settings = RecorderFileSettings(),
			onEvent = {},
			contentPadding = PaddingValues(12.dp)
		)
	}
}
package com.eva.recorderapp.voice_recorder.presentation.settings.composables.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderAudioSettings
import com.eva.recorderapp.voice_recorder.presentation.settings.composables.SettingsItemWithSwitch
import com.eva.recorderapp.voice_recorder.presentation.settings.utils.AudioSettingsEvent

@Composable
fun AudioSettings(
	settings: RecorderAudioSettings,
	onEvent: (AudioSettingsEvent) -> Unit, modifier: Modifier = Modifier
) {
	LazyColumn(
		modifier = modifier.fillMaxSize(),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		item {
			AudioEncoderSelector(
				encoder = settings.encoders,
				onEncoderChange = { onEvent(AudioSettingsEvent.OnEncoderChange(it)) },
			)
		}
		item {
			AudioQualitySelector(
				quality = settings.quality,
				onQualityChanged = { onEvent(AudioSettingsEvent.OnQualityChange(it)) }
			)
		}
		item {
			SettingsItemWithSwitch(
				isSelected = settings.enableStero,
				title = stringResource(id = R.string.recording_settings_enable_stereo),
				text = stringResource(id = R.string.recording_settings_enable_stereo_text),
				leading = {
					Icon(
						painter = painterResource(id = R.drawable.ic_channel),
						contentDescription = stringResource(id = R.string.recording_settings_skip_silences_title),
					)
				},
				onSelect = { onEvent(AudioSettingsEvent.OnStereoModeChange(it)) },
			)
		}
		item {
			SettingsItemWithSwitch(
				isSelected = settings.skipSilences,
				title = stringResource(id = R.string.recording_settings_skip_silences_title),
				text = stringResource(id = R.string.recording_settings_skip_silences_text),
				leading = {
					Icon(
						painter = painterResource(id = R.drawable.ic_silence),
						contentDescription = stringResource(id = R.string.recording_settings_skip_silences_title),
					)
				},
				onSelect = { onEvent(AudioSettingsEvent.OnSkipSilencesChange(it)) },

				)
		}
		item {
			SettingsItemWithSwitch(
				isSelected = settings.blockCallsDuringRecording,
				title = stringResource(id = R.string.recording_settings_block_calls),
				text = stringResource(id = R.string.recording_settings_block_calls_text),
				leading = {
					Icon(
						imageVector = Icons.Outlined.Phone,
						contentDescription = stringResource(id = R.string.recording_settings_block_calls),
					)
				},
				onSelect = { onEvent(AudioSettingsEvent.OnSkipSilencesChange(it)) },
			)
		}
	}
}

@PreviewLightDark
@Composable
private fun AudioSettingsPreview() = RecorderAppTheme {
	Surface {
		AudioSettings(
			settings = RecorderAudioSettings(),
			onEvent = {},
			modifier = Modifier.padding(12.dp)
		)
	}
}
package com.eva.feature_settings.composables.audio

import android.os.PowerManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.eva.datastore.domain.models.RecorderAudioSettings
import com.eva.feature_settings.composables.SettingsItemWithSwitch
import com.eva.feature_settings.utils.AudioSettingsEvent
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme

@Composable
internal fun AudioSettingsTabContent(
	settings: RecorderAudioSettings,
	onEvent: (AudioSettingsEvent) -> Unit,
	modifier: Modifier = Modifier,
	contentPadding: PaddingValues = PaddingValues(12.dp),
) {

	val isInspectionMode = LocalInspectionMode.current
	val context = LocalContext.current

	val isIgnoreOptimization = remember(context) {
		if (isInspectionMode) return@remember false
		val powerManager = context.getSystemService<PowerManager>()
		powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
	}

	LazyColumn(
		modifier = modifier.fillMaxSize(),
		contentPadding = contentPadding,
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		if (!isIgnoreOptimization) {
			item {
				IgnoreBatteryOptimizationCard(
					modifier = Modifier.animateItem()
				)
			}
		}

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
				isSelected = settings.enableStereo,
				title = stringResource(id = R.string.recording_settings_enable_stereo),
				text = stringResource(id = R.string.recording_settings_enable_stereo_text),
				leading = {
					Icon(
						painter = painterResource(id = R.drawable.ic_stereo),
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
				onSelect = {
					onEvent(AudioSettingsEvent.OnSkipSilencesChange(it))
				},
			)
		}
		item {
			PauseRecorderOnCallTile(
				isPauseRecordingOnIncommingCall = settings.pauseRecordingOnCall,
				onActionEnabledChanged = { onEvent(AudioSettingsEvent.OnPauseRecorderOnCalls(it)) })
		}
		item {
			LocationInfoCollectionCard(
				isAddLocationInfoAllowed = settings.addLocationInfoInRecording,
				onActionEnabledChanged = { onEvent(AudioSettingsEvent.OnAddLocationEnabled(it)) })
		}
		item {
			SettingsItemWithSwitch(
				isSelected = settings.useBluetoothMic,
				title = stringResource(id = R.string.recording_settings_use_bt_mic),
				text = stringResource(id = R.string.recording_settings_use_bt_mic_text),
				leading = {
					Icon(
						painter = painterResource(id = R.drawable.ic_bluetooth),
						contentDescription = stringResource(id = R.string.recording_settings_use_bt_mic),
					)
				},
				onSelect = {
					onEvent(AudioSettingsEvent.OnUseBluetoothMicChanged(it))
				},
			)
		}
	}
}

@PreviewLightDark
@Composable
private fun AudioSettingsTabContentPreview() = RecorderAppTheme {
	Surface {
		AudioSettingsTabContent(
			settings = RecorderAudioSettings(),
			onEvent = {},
		)
	}
}
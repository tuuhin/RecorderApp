package com.eva.feature_settings.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.eva.datastore.domain.models.RecorderAudioSettings
import com.eva.datastore.domain.models.RecorderFileSettings
import com.eva.datastore.domain.models.TranscriptionSettings
import com.eva.feature_settings.SettingsPreviewFakes
import com.eva.feature_settings.composables.SettingsScreenTopAppbar
import com.eva.feature_settings.composables.SettingsTabContent
import com.eva.feature_settings.composables.audio.AudioSettingsTabContent
import com.eva.feature_settings.composables.files.FileSettingsTabContent
import com.eva.feature_settings.composables.transcribe.TranscribeSettingsTabContent
import com.eva.feature_settings.utils.AudioSettingsEvent
import com.eva.feature_settings.utils.FileSettingsChangeEvent
import com.eva.feature_settings.utils.SettingsTabs
import com.eva.feature_settings.utils.TranscriptionSettingsEvents
import com.eva.recordings.domain.models.DeviceTotalStorageModel
import com.eva.transcribe.domain.models.STTModel
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioSettingsScreen(
	audioSettings: RecorderAudioSettings,
	fileSettings: RecorderFileSettings,
	transcriptionSettings: TranscriptionSettings,
	sttModels: ImmutableList<STTModel>,
	onTranscriptionSettingChange: (TranscriptionSettingsEvents) -> Unit,
	onFileSettingsChange: (FileSettingsChangeEvent) -> Unit,
	onAudioSettingsChange: (AudioSettingsEvent) -> Unit,
	modifier: Modifier = Modifier,
	initialTab: SettingsTabs = SettingsTabs.AUDIO_SETTINGS,
	storageModel: DeviceTotalStorageModel = DeviceTotalStorageModel(),
	navigation: @Composable () -> Unit = {},
	onNavigateToInfo: () -> Unit = {},
) {

	val snackBarProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior ()

	Scaffold(
		topBar = {
			SettingsScreenTopAppbar(
				navigation = navigation,
				onNavigateToInfo = onNavigateToInfo,
				scrollBehavior = scrollBehavior
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
	) { scPadding ->
		SettingsTabContent(
			initialTab = initialTab,
			audioSettings = {
				AudioSettingsTabContent(
					settings = audioSettings,
					onEvent = onAudioSettingsChange,
					contentPadding = PaddingValues(all = dimensionResource(R.dimen.sc_padding)),
				)
			},
			filesSettings = {
				FileSettingsTabContent(
					settings = fileSettings,
					model = storageModel,
					onEvent = onFileSettingsChange,
					contentPadding = PaddingValues(all = dimensionResource(R.dimen.sc_padding)),
				)
			},
			transcriptionSettings = {
				TranscribeSettingsTabContent(
					sttModels = sttModels,
					onEvent = onTranscriptionSettingChange,
					settings = transcriptionSettings,
					contentPadding = PaddingValues(dimensionResource(R.dimen.sc_padding))
				)
			},
			contentPadding = scPadding,
			modifier = Modifier.fillMaxSize(),
		)
	}
}


private class SettingsTabPreviewParams :
	CollectionPreviewParameterProvider<SettingsTabs>(SettingsTabs.entries)

@Preview
@Composable
private fun AudioSettingsScreenPreview(
	@PreviewParameter(SettingsTabPreviewParams::class)
	initialTab: SettingsTabs,
) = RecorderAppTheme {
	AudioSettingsScreen(
		audioSettings = RecorderAudioSettings(),
		fileSettings = RecorderFileSettings(),
		transcriptionSettings = TranscriptionSettings(),
		sttModels = SettingsPreviewFakes.STT_MODELS_LIST.toImmutableList(),
		onTranscriptionSettingChange = {},
		onAudioSettingsChange = {},
		onFileSettingsChange = {},
		initialTab = initialTab,
		navigation = {
			Icon(
				imageVector = Icons.AutoMirrored.Default.ArrowBack,
				contentDescription = stringResource(R.string.back_arrow)
			)
		},
	)
}
package com.eva.recorderapp.voice_recorder.presentation.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.eva.recorderapp.R
import com.eva.recorderapp.ui.theme.RecorderAppTheme
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderAudioSettings
import com.eva.recorderapp.voice_recorder.domain.datastore.models.RecorderFileSettings
import com.eva.recorderapp.voice_recorder.presentation.settings.composables.SettingsTabContent
import com.eva.recorderapp.voice_recorder.presentation.settings.composables.audio.AudioSettings
import com.eva.recorderapp.voice_recorder.presentation.settings.composables.files.FileSettings
import com.eva.recorderapp.voice_recorder.presentation.settings.utils.AudioSettingsEvent
import com.eva.recorderapp.voice_recorder.presentation.settings.utils.FileSettingsChangeEvent
import com.eva.recorderapp.voice_recorder.presentation.settings.utils.SettingsTabs
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsScreen(
	audioSettings: RecorderAudioSettings,
	fileSettings: RecorderFileSettings,
	onFileSettingsChange: (FileSettingsChangeEvent) -> Unit,
	onAudioSettingsChange: (AudioSettingsEvent) -> Unit,
	modifier: Modifier = Modifier,
	initialTab: SettingsTabs = SettingsTabs.AUDIO_SETTINGS,
	navigation: @Composable () -> Unit = {},
	onNavigateToInfo: () -> Unit = {},
) {

	val snackBarProvider = LocalSnackBarProvider.current
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(text = stringResource(id = R.string.app_settings_common)) },
				navigationIcon = navigation,
				actions = {
					IconButton(onClick = onNavigateToInfo) {
						Icon(
							painter = painterResource(id = R.drawable.ic_info),
							contentDescription = stringResource(R.string.extras_info)
						)
					}
				}
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarProvider) },
		modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
	) { scPadding ->
		SettingsTabContent(
			initialTab = initialTab,
			audioSettings = {
				AudioSettings(
					settings = audioSettings,
					onEvent = onAudioSettingsChange,
					contentPadding = PaddingValues(all = dimensionResource(R.dimen.sc_padding)),
				)
			},
			filesSettings = {
				FileSettings(
					settings = fileSettings,
					onEvent = onFileSettingsChange,
					contentPadding = PaddingValues(all = dimensionResource(R.dimen.sc_padding)),
				)
			},
			contentPadding = scPadding,
			modifier = Modifier.fillMaxSize(),
		)
	}
}


private class SettingsTabPreviewParams :
	CollectionPreviewParameterProvider<SettingsTabs>(SettingsTabs.entries)

@PreviewLightDark
@Composable
private fun AudioSettingsScreenPreview(
	@PreviewParameter(SettingsTabPreviewParams::class)
	initialTab: SettingsTabs
) = RecorderAppTheme {
	AudioSettingsScreen(
		audioSettings = RecorderAudioSettings(),
		fileSettings = RecorderFileSettings(),
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
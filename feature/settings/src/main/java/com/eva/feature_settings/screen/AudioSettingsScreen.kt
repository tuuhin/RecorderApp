package com.eva.feature_settings.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.eva.datastore.domain.models.RecorderAudioSettings
import com.eva.datastore.domain.models.RecorderFileSettings
import com.eva.feature_settings.composables.SettingsTabContent
import com.eva.feature_settings.composables.audio.AudioSettingsTabContent
import com.eva.feature_settings.composables.files.FileSettingsTabContent
import com.eva.feature_settings.utils.AudioSettingsEvent
import com.eva.feature_settings.utils.FileSettingsChangeEvent
import com.eva.feature_settings.utils.SettingsTabs
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import com.eva.ui.utils.LocalSnackBarProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioSettingsScreen(
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
							imageVector = Icons.Outlined.Info,
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
				AudioSettingsTabContent(
					settings = audioSettings,
					onEvent = onAudioSettingsChange,
					contentPadding = PaddingValues(all = dimensionResource(R.dimen.sc_padding)),
				)
			},
			filesSettings = {
				FileSettingsTabContent(
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
	initialTab: SettingsTabs,
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
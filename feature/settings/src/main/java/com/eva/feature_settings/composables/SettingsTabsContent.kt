package com.eva.feature_settings.composables

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eva.feature_settings.utils.SettingsTabs
import com.eva.ui.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTabContent(
	audioSettings: @Composable () -> Unit,
	filesSettings: @Composable () -> Unit,
	modifier: Modifier = Modifier,
	initialTab: SettingsTabs = SettingsTabs.AUDIO_SETTINGS,
	contentPadding: PaddingValues = PaddingValues(0.dp),
) {
	val scope = rememberCoroutineScope()

	val pagerState = rememberPagerState(
		initialPage = initialTab.tabIndex,
		pageCount = { SettingsTabs.entries.size }
	)

	val selectedTabIndex by remember(pagerState) {
		derivedStateOf(pagerState::currentPage)
	}

	Column(
		modifier = modifier.padding(contentPadding),
		verticalArrangement = Arrangement.spacedBy(2.dp)
	) {
		PrimaryTabRow(
			selectedTabIndex = selectedTabIndex,
		) {
			SettingsTabs.entries.forEach { tab ->
				Tab(
					selected = tab.tabIndex == selectedTabIndex,
					onClick = {
						val index = tab.tabIndex
						if (index != selectedTabIndex) scope.launch {
							pagerState.animateScrollToPage(index)
						}
					},
					text = { Text(text = stringResource(tab.stringRes)) },
				)
			}
		}

		HorizontalPager(
			state = pagerState,
			flingBehavior = PagerDefaults.flingBehavior(
				state = pagerState,
				snapPositionalThreshold = .4f,
				snapAnimationSpec = spring(
					dampingRatio = Spring.DampingRatioNoBouncy,
					stiffness = Spring.StiffnessLow
				)
			),
			pageSpacing = dimensionResource(id = R.dimen.sc_padding_secondary),
			modifier = Modifier.fillMaxSize(),
		) { idx ->
			when (idx) {
				SettingsTabs.AUDIO_SETTINGS.tabIndex -> audioSettings()
				SettingsTabs.FILES_SETTINGS.tabIndex -> filesSettings()
				else -> {}
			}
		}
	}
}
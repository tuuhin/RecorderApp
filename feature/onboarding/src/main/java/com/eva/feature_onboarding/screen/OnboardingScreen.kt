package com.eva.feature_onboarding.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eva.feature_onboarding.composables.FeaturesPage
import com.eva.feature_onboarding.composables.ImportantPermissionsPage
import com.eva.feature_onboarding.composables.OnBoardingBottomBar
import com.eva.feature_onboarding.composables.WelcomePage
import com.eva.ui.R
import com.eva.ui.theme.RecorderAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OnboardingScreen(
	modifier: Modifier = Modifier,
	onContinueToApp: () -> Unit = {},
) {

	val pagerState = rememberPagerState { 3 }
	val scope = rememberCoroutineScope()

	val progress by remember(pagerState) {
		derivedStateOf {
			val current = pagerState.currentPage
			val count = pagerState.pageCount
			(current.toFloat() / count).coerceIn(0f..1f)
		}
	}

	val showPreviousButton by remember(pagerState) {
		derivedStateOf { pagerState.currentPage != 0 }
	}

	val showNextOrContinueButton by remember(pagerState) {
		derivedStateOf { pagerState.currentPage < pagerState.pageCount }
	}

	val isContinueAction by remember(pagerState) {
		derivedStateOf { pagerState.currentPage + 1 == pagerState.pageCount }
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = {},
				actions = {
					TextButton(onClick = onContinueToApp) {
						Text(
							text = stringResource(R.string.action_skip),
							style = MaterialTheme.typography.titleMedium
						)
					}
				},
			)
		},
		bottomBar = {
			OnBoardingBottomBar(
				progress = { progress },
				showPreviousButton = showPreviousButton,
				showNextOrContinueActionButton = showNextOrContinueButton,
				showContinueAction = isContinueAction,
				onContinueToContent = onContinueToApp,
				onPrevious = {
					val previous = pagerState.currentPage - 1
					if (previous >= 0) {
						scope.launch { pagerState.animateScrollToPage(previous) }
					}
				},
				onNext = {
					val next = pagerState.currentPage + 1
					if (next < pagerState.pageCount) {
						scope.launch { pagerState.animateScrollToPage(next) }
					}
				},
				contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.sc_padding))
			)
		},
		modifier = modifier,
	) { scPadding ->
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
			contentPadding = scPadding,
			pageSpacing = dimensionResource(id = R.dimen.sc_padding_secondary),
			modifier = Modifier
				.fillMaxSize()
				.padding(dimensionResource(R.dimen.sc_padding)),
		) { idx ->
			when (idx) {
				1 -> FeaturesPage()
				2 -> ImportantPermissionsPage()
				else -> WelcomePage()
			}
		}
	}
}

@PreviewLightDark
@Composable
private fun OnboardingScreenPreview() = RecorderAppTheme() {
	OnboardingScreen()
}
package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.categories.SelectCategoryScreen
import com.eva.recorderapp.voice_recorder.presentation.categories.SelectCategoryViewModel
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSharedTransitionVisibilityScopeProvider

fun NavGraphBuilder.selectRecordingCategoryRoute(
	controller: NavController,
) = animatedComposable<NavRoutes.SelectRecordingCategoryRoute> {

	val viewModel = hiltViewModel<SelectCategoryViewModel>()

	UiEventsSideEffect(
		eventsFlow = viewModel::uiEvent,
		onPopScreenEvent = dropUnlessResumed(block = controller::popBackStack)
	)

	val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
	val categories by viewModel.categories.collectAsStateWithLifecycle()
	val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

	CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
		SelectCategoryScreen(
			isLoaded = isLoaded,
			categories = categories,
			selectedCategory = selectedCategory,
			onEvent = viewModel::onEvent,
			onNavigateToCreateNew = dropUnlessResumed {
				controller.navigate(NavRoutes.CreateOrUpdateCategory())
			},
			navigation = {
				if (controller.previousBackStackEntry?.destination?.route != null) {
					IconButton(
						onClick = dropUnlessResumed(block = controller::popBackStack)
					) {
						Icon(
							imageVector = Icons.AutoMirrored.Default.ArrowBack,
							contentDescription = stringResource(R.string.back_arrow)
						)
					}
				}
			},
		)
	}
}
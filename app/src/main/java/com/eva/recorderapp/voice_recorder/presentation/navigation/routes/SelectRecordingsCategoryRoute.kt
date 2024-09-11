package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.categories.SelectRecordingCategoryViewModel
import com.eva.recorderapp.voice_recorder.presentation.categories.SelectRecordingsCategoryScreen
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable

fun NavGraphBuilder.selectRecordingCategoryRoute(
	controller: NavController,
) = animatedComposable<NavRoutes.SelectRecordingCategoryRoute> {

	val viewModel = hiltViewModel<SelectRecordingCategoryViewModel>()

	UiEventsSideEffect(
		eventsFlow = viewModel::uiEvent,
		onPopScreenEvent = dropUnlessResumed(block = controller::popBackStack)
	)

	val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
	val categories by viewModel.categories.collectAsStateWithLifecycle()
	val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

	SelectRecordingsCategoryScreen(
		isLoaded = isLoaded,
		categories = categories,
		selectedCategory = selectedCategory,
		onEvent = viewModel::onEvent,
		navigation = {
			IconButton(
				onClick = dropUnlessResumed(block = controller::popBackStack)
			) {
				Icon(
					imageVector = Icons.AutoMirrored.Default.ArrowBack,
					contentDescription = stringResource(R.string.back_arrow)
				)
			}
		},
	)
}
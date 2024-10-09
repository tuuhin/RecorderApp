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
import com.eva.recorderapp.voice_recorder.presentation.categories.create_category.CreateOrEditCategoryScreen
import com.eva.recorderapp.voice_recorder.presentation.categories.create_category.CreateOrUpdateCategoryViewModel
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable

fun NavGraphBuilder.createOrUpdateCategoryRoute(
	controller: NavController,
) = animatedComposable<NavRoutes.CreateOrUpdateCategory>(
) {

	val viewModel = hiltViewModel<CreateOrUpdateCategoryViewModel>()

	UiEventsSideEffect(
		eventsFlow = viewModel::uiEvent,
		onPopScreenEvent = dropUnlessResumed(block = controller::popBackStack)
	)

	val state by viewModel.createState.collectAsStateWithLifecycle()

	CreateOrEditCategoryScreen(
		state = state,
		onEvent = viewModel::onEvent,
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
		}
	)
}
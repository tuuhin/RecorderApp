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
import com.eva.recorderapp.voice_recorder.presentation.categories.ManageCategoriesScreen
import com.eva.recorderapp.voice_recorder.presentation.categories.ManageCategoryViewModel
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable


fun NavGraphBuilder.recordingCategories(
	controller: NavController,
) = animatedComposable<NavRoutes.ManageCategories> {

	val viewModel = hiltViewModel<ManageCategoryViewModel>()

	val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
	val categories by viewModel.categories.collectAsStateWithLifecycle()
	val createState by viewModel.createState.collectAsStateWithLifecycle()

	UiEventsSideEffect(eventsFlow = viewModel::uiEvent)

	ManageCategoriesScreen(
		isLoaded = isLoaded,
		categories = categories,
		createOrEditState = createState,
		onScreenEvent = viewModel::onScreenEvent,
		onCreateOrEditEvent = viewModel::onCreateOrEdit,
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
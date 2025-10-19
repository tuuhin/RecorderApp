package com.eva.feature_categories.routes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.eva.feature_categories.category_picker.CategoryPickerScreen
import com.eva.feature_categories.category_picker.CategoryPickerViewModel
import com.eva.ui.R
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.LocalSharedTransitionVisibilityScopeProvider
import com.eva.ui.utils.UiEventsHandler

fun NavGraphBuilder.categoryPickerRoute(controller: NavController) =
	animatedComposable<NavRoutes.SelectRecordingCategoryRoute> {

		val viewModel = hiltViewModel<CategoryPickerViewModel>()

		UiEventsHandler(
			eventsFlow = viewModel::uiEvent,
			onNavigateBack = dropUnlessResumed(block = controller::popBackStack)
		)

		val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
		val categories by viewModel.categories.collectAsStateWithLifecycle()
		val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

		CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
			CategoryPickerScreen(
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
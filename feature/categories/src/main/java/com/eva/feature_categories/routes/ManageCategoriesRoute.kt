package com.eva.feature_categories.routes

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
import com.eva.feature_categories.manage_category.ManageCategoriesScreen
import com.eva.feature_categories.manage_category.ManageCategoryViewModel
import com.eva.ui.R
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.LocalSharedTransitionVisibilityScopeProvider
import com.eva.ui.utils.UiEventsHandler


fun NavGraphBuilder.manageRecordingCategories(controller: NavController) =
	animatedComposable<NavRoutes.ManageCategories> {

		val viewModel = hiltViewModel<ManageCategoryViewModel>()

		val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
		val categories by viewModel.categories.collectAsStateWithLifecycle()

		UiEventsHandler(eventsFlow = viewModel::uiEvent)

		CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
			ManageCategoriesScreen(
				isLoaded = isLoaded,
				categories = categories,
				onScreenEvent = viewModel::onScreenEvent,
				onNavigateToCreateCategory = dropUnlessResumed {
					controller.navigate(NavRoutes.CreateOrUpdateCategory())
				},
				onNavigateToEditCategory = { category ->
					controller.navigate(NavRoutes.CreateOrUpdateCategory(category.id))
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
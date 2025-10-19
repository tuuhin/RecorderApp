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
import androidx.navigation.toRoute
import com.eva.feature_categories.create_category.CreateCategoryScreen
import com.eva.feature_categories.create_category.CreateCategoryViewModel
import com.eva.ui.R
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.navigation.animatedComposable
import com.eva.ui.utils.LocalSharedTransitionVisibilityScopeProvider
import com.eva.ui.utils.UiEventsHandler

fun NavGraphBuilder.createOrEditCategoryRoute(controller: NavController) =
	animatedComposable<NavRoutes.CreateOrUpdateCategory> {

		val viewModel = hiltViewModel<CreateCategoryViewModel>()
		val route = it.toRoute<NavRoutes.CreateOrUpdateCategory>()

		UiEventsHandler(
			eventsFlow = viewModel::uiEvent,
			onNavigateBack = dropUnlessResumed(block = controller::popBackStack)
		)

		val state by viewModel.createState.collectAsStateWithLifecycle()

		CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
			CreateCategoryScreen(
				categoryId = route.categoryId ?: -1,
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
	}
package com.eva.recorderapp.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.eva.feature_categories.routes.categoryPickerRoute
import com.eva.feature_categories.routes.createOrEditCategoryRoute
import com.eva.feature_categories.routes.manageRecordingCategories
import com.eva.feature_recorder.recorderRoute
import com.eva.feature_recordings.bin.trashRecordingsRoute
import com.eva.feature_recordings.recordings.recordingsRoute
import com.eva.feature_recordings.rename.renameRecordingDialog
import com.eva.feature_recordings.search.recordingsSearchRoute
import com.eva.feature_settings.settingsRoute
import com.eva.recorderapp.navigation.navgraph.playerNavGraph
import com.eva.recorderapp.navigation.routes.appInfoDialog
import com.eva.ui.navigation.NavRoutes
import com.eva.ui.utils.LocalSharedTransitionScopeProvider
import com.eva.ui.utils.LocalSnackBarProvider


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(
	modifier: Modifier = Modifier,
	onSetController: suspend (NavHostController) -> Unit = {},
) {
	val navController = rememberNavController()
	val currentOnSetController by rememberUpdatedState(onSetController)

	LaunchedEffect(navController) {
		currentOnSetController(navController)
	}

	val snackBarProvider = remember { SnackbarHostState() }

	SharedTransitionLayout {
		CompositionLocalProvider(
			LocalSnackBarProvider provides snackBarProvider,
			LocalSharedTransitionScopeProvider provides this,
		) {
			NavHost(
				navController = navController,
				startDestination = NavRoutes.VoiceRecorder,
				modifier = modifier
			) {
				// screens
				recorderRoute(navController = navController)
				recordingsRoute(controller = navController)
				trashRecordingsRoute(controller = navController)
				recordingsSearchRoute(controller = navController)
				manageRecordingCategories(controller = navController)
				settingsRoute(controller = navController)
				categoryPickerRoute(controller = navController)
				createOrEditCategoryRoute(controller = navController)
				//dialogs
				appInfoDialog()
				renameRecordingDialog(controller = navController)
				// subgraph
				playerNavGraph(controller = navController)
			}
		}
	}
}


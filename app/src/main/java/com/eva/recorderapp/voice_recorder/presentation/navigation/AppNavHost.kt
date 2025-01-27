package com.eva.recorderapp.voice_recorder.presentation.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.eva.recorderapp.voice_recorder.presentation.navigation.dialogs.appInfoDialog
import com.eva.recorderapp.voice_recorder.presentation.navigation.dialogs.renameRecordingDialog
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.audioEditorRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.audioPlayerRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.audioSettingsRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.createOrUpdateCategoryRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.recorderRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.recordingCategories
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.recordingsRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.recordingsSearchRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.selectRecordingCategoryRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.trashRecordingsRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSharedTransitionScopeProvider
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(
	modifier: Modifier = Modifier,
	navController: NavHostController = rememberNavController(),
) {
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
				recordingCategories(controller = navController)
				audioPlayerRoute(controller = navController)
				audioEditorRoute(controller = navController)
				audioSettingsRoute(controller = navController)
				selectRecordingCategoryRoute(controller = navController)
				createOrUpdateCategoryRoute(controller = navController)
				//dialogs
				appInfoDialog()
				renameRecordingDialog(controller = navController)
			}
		}
	}
}



package com.eva.recorderapp.voice_recorder.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.eva.recorderapp.voice_recorder.presentation.navigation.dialogs.appInfoDialog
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.audioEditorRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.audioPlayerRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.audioSettingsRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.recorderRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.recordingsroute
import com.eva.recorderapp.voice_recorder.presentation.navigation.routes.trashRecordingsRoute
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.util.LocalSnackBarProvider

@Composable
fun AppNavHost(
	modifier: Modifier = Modifier,
	navController: NavHostController = rememberNavController(),
) {
	val snackBarProvider = remember { SnackbarHostState() }

	CompositionLocalProvider(
		value = LocalSnackBarProvider provides snackBarProvider
	) {
		NavHost(
			navController = navController,
			startDestination = NavRoutes.VoiceRecorder,
			modifier = modifier
		) {
			recorderRoute(navController = navController)
			recordingsroute(controller = navController)
			trashRecordingsRoute(controller = navController)
			audioPlayerRoute(controller = navController)
			audioEditorRoute(controller = navController)
			audioSettingsRoute(controller = navController)
			//dialog
			appInfoDialog()
		}
	}
}



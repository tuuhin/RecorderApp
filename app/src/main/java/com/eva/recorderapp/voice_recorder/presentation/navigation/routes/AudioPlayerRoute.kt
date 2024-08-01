package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.record_player.AudioPlayerScreen
import com.eva.recorderapp.voice_recorder.presentation.record_player.AudioPlayerViewModel

fun NavGraphBuilder.audioPlayerRoute(
	controller: NavHostController
) = composable<NavRoutes.AudioPlayer>(
	deepLinks = listOf(
		navDeepLink {
			uriPattern = NavDeepLinks.appPlayerDestinationPattern
			action = Intent.ACTION_VIEW
		},
		navDeepLink {
			uriPattern = NavDeepLinks.externalAudioDestinationPattern
			action = Intent.ACTION_VIEW
			mimeType = "audio/*"
		}
	)
) {

	val viewModel = hiltViewModel<AudioPlayerViewModel>()
	val contentState by viewModel.contentLoadState.collectAsStateWithLifecycle()

	UiEventsSideEffect(viewModel = viewModel)

	AudioPlayerScreen(
		loadState = contentState,
		onPlayerEvents = viewModel::onPlayerEvents,
		onNavigateToEdit = {
			controller.navigate(NavRoutes.AudioEditor)
		},
		navigation = {
			IconButton(
				onClick = dropUnlessResumed {
					if (controller.previousBackStackEntry?.destination?.route != null)
						controller.popBackStack()
				}
			) {
				Icon(
					imageVector = Icons.AutoMirrored.Default.ArrowBack,
					contentDescription = stringResource(R.string.back_arrow)
				)
			}
		},
	)
}

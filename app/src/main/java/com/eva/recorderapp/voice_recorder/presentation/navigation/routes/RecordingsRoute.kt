package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navDeepLink
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.data.recordings.provider.RecordingsProvider
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavDeepLinks
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable
import com.eva.recorderapp.voice_recorder.presentation.recordings.RecordingsScreen
import com.eva.recorderapp.voice_recorder.presentation.recordings.RecordingsViewmodel
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.DeleteOrTrashRecordingsRequest

fun NavGraphBuilder.recordingsRoute(
	controller: NavController
) = animatedComposable<NavRoutes.VoiceRecordings>(
	deepLinks = listOf(
		navDeepLink {
			uriPattern = NavDeepLinks.recordingsDestinationPattern
			action = Intent.ACTION_VIEW
		},
	),
) {

	val viewModel = hiltViewModel<RecordingsViewmodel>()
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	val launcher = rememberLauncherForActivityResult(
		ActivityResultContracts.StartIntentSenderForResult()
	) { result ->
		val message = if (result.resultCode == Activity.RESULT_OK)
			context.getString(R.string.recording_delete_request_success)
		else context.getString(R.string.recording_delete_request_failed)

		Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
	}

	LaunchedEffect(viewModel, lifecycleOwner) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@LaunchedEffect

		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			viewModel.trashRequestEvent.collect { event ->
				when (event) {
					is DeleteOrTrashRecordingsRequest.OnTrashRequest -> {
						val request = RecordingsProvider
							.createTrashRequest(context, event.recordings)

						launcher.launch(request)
					}

					else -> {}
				}
			}
		}
	}

	UiEventsSideEffect(viewModel = viewModel)

	val recordings by viewModel.recordings.collectAsStateWithLifecycle()
	val isRecordingsLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
	val sortInfo by viewModel.sortInfo.collectAsStateWithLifecycle()
	val renameState by viewModel.renameState.collectAsStateWithLifecycle()

	RecordingsScreen(
		isRecordingsLoaded = isRecordingsLoaded,
		recordings = recordings,
		sortInfo = sortInfo,
		renameState = renameState,
		onScreenEvent = viewModel::onScreenEvent,
		onRenameEvent = viewModel::onRenameRecordingEvent,
		onNavigateToBin = dropUnlessResumed {
			controller.navigate(NavRoutes.TrashRecordings)
		},
		onRecordingSelect = { record ->
			val audioRoute = NavRoutes.AudioPlayer(record.id)
			controller.navigate(audioRoute)
		},
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
package com.eva.recorderapp.voice_recorder.presentation.navigation.routes

import android.app.Activity
import android.os.Build
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
import com.eva.recorderapp.R
import com.eva.recorderapp.voice_recorder.data.recordings.provider.RecordingsProvider
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.NavRoutes
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.UiEventsSideEffect
import com.eva.recorderapp.voice_recorder.presentation.navigation.util.animatedComposable
import com.eva.recorderapp.voice_recorder.presentation.recordings.RecordingsBinScreen
import com.eva.recorderapp.voice_recorder.presentation.recordings.RecordingsBinViewmodel
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.DeleteOrTrashRecordingsRequest
import com.eva.recorderapp.voice_recorder.presentation.recordings.util.event.TrashRecordingScreenEvent

fun NavGraphBuilder.trashRecordingsRoute(
	controller: NavController
) = animatedComposable<NavRoutes.TrashRecordings> {

	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current

	val viewModel = hiltViewModel<RecordingsBinViewmodel>()

	val deleteRequestLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartIntentSenderForResult(),
		onResult = { result ->
			val message =
				if (result.resultCode == Activity.RESULT_OK)
					context.getString(R.string.recording_delete_request_success)
				else context.getString(R.string.recording_delete_request_failed)

			val event = TrashRecordingScreenEvent.OnPostDeleteRequestApi30(message)

			viewModel.onScreenEvent(event)
		},
	)

	LaunchedEffect(viewModel, lifecycleOwner) {
		lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
			viewModel.deleteRequestEvent.collect { event ->

				when (event) {
					is DeleteOrTrashRecordingsRequest.OnDeleteRequest -> {
						if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
							event.intentSenderRequest?.let { request ->
								deleteRequestLauncher.launch(request)
							}
						} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
							val request = RecordingsProvider.createDeleteRequest(
								context,
								event.trashRecordings
							)
							// launch the request
							deleteRequestLauncher.launch(request)
						}
					}

					else -> {}
				}
			}
		}
	}

	UiEventsSideEffect(viewModel = viewModel)

	val recordings by viewModel.trashRecordings.collectAsStateWithLifecycle()
	val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()

	RecordingsBinScreen(
		isRecordingsLoaded = isLoaded,
		recordings = recordings,
		onScreenEvent = viewModel::onScreenEvent,
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